import spacy
import json
import re
from spacy.matcher import Matcher
from nltk.stem import *

# CONSTANTS
accepted_pos = ["ADJ", "VERB", "NOUN", "PROPN"]
rejected_pos = ["AUX", "CONJ", "CCONJ", "DET", "INTJ", "SCONJ"]
text_to_noun_helper = " is big"
initials_regex = re.compile(r"([A-Z][.])+")

# MODEL
nlp = nlp = spacy.load("en_core_web_sm")
lancaster=LancasterStemmer()

# ################FUNCTIONS#################

"""
Checks iff all the tokens are proper nouns
"""
def all_propn_p(term):
    all_p = True
    doc = nlp(term)
    for token in doc:
        if token.pos_ != 'PUNCT' and  token.pos_ != "PROPN":
            all_p = False
            break
    return all_p


"""
Checks the tokens that can be initials for first names
"""
def initials(noun_tokens):
    initials_list = []
    if len(noun_tokens) > 0:
        for i in range(len(noun_tokens)):
            for token in noun_tokens[i]:
                if initials_regex.match(token.text):
                    initials_list.append(token)
    return initials_list

def compare_tokens(token1, token2):
    if token1.lemma_.lower() == token2.lemma_.lower() \
        or token1.text.lower() == token2.text.lower() \
        or lancaster.stem(token1.text.lower()) == lancaster.stem(token2.text.lower()):
        return True
    else:
        return False


def compare_chunks(term_chunk_tokens, sentence_chunks_tokens, sentence_doc, propn_term, initials_list):

#     print("sentence  doc: ", sentence_doc)
#     print("comparing: ", term_chunk_tokens, " VS. ", sentence_chunks_tokens)
    for i in range(0, len(sentence_chunks_tokens)):
        match_vector = compare_chunk_tokens(term_chunk_tokens, sentence_chunks_tokens[i], sentence_doc)
#         print("match_vector", match_vector)
        if is_match(match_vector, term_chunk_tokens, propn_term, initials_list):
            return match_vector
    return False


def compare_chunk_tokens(term_chunk_tokens, sentence_chunk_tokens, sentence_doc):
    matches = 0
    start_index = 0
    match_vector = initialize_match_vector(len(term_chunk_tokens))
    all_from_sentence = True
    for term_token_index in range(0, len(term_chunk_tokens)):
        all_from_sentence = True
        # print("s: ", sentence_chunk_tokens)
        for sentence_token_index in range(start_index, len(sentence_chunk_tokens)):
            # print("C -> ", term_chunk_tokens[term_token_index].lemma_.lower(), sentence_chunk_tokens[sentence_token_index].lemma_.lower())
            # print("C -> ", term_chunk_tokens[term_token_index].pos_,sentence_chunk_tokens[sentence_token_index].pos_)
            if compare_tokens(term_chunk_tokens[term_token_index], sentence_chunk_tokens[sentence_token_index]):
                # print("\t", "match:", term_chunk_tokens[term_token_index].lemma_,
                #      sentence_chunk_tokens[sentence_token_index].lemma_, sep=" ")
                start_index = sentence_token_index + 1
                match_vector[term_token_index] = sentence_chunk_tokens[sentence_token_index].i
                matches += 1
                break
            else:
                # print("####")
                all_from_sentence = False

    # print("VECTOR (before) --> ", match_vector)
    # if matches == len(term_chunk_tokens):
    #     print("MATCH!")

    if all_from_sentence and len(sentence_chunk_tokens) > 0:
        # print("additional check")
        match_vector = additional_check(term_chunk_tokens, match_vector, sentence_doc)


    # print("VECTOR (after) --> ", match_vector)
    return match_vector


def is_match(match_vector, term_chunk_tokens, propn_term, initials_list):
    if propn_term:
        valid_result = False
        for i in range(len(match_vector)):
            if match_vector[i] != -1:
                # print("I", initials_list, term_chunk_tokens[i], not term_chunk_tokens[i] in initials_list)
                if not term_chunk_tokens[i] in initials_list:
                    valid_result = True
        return valid_result
    else:
        for match in match_vector:
            if match == -1:
                return False
        return True


def additional_check(term_chunk_tokens, match_vector, sentence_doc):
    if len(match_vector) > 0:
        previous_index = match_vector[0]
        go_backwards = False
        for i in range(0, len(term_chunk_tokens)):
            if match_vector[i] == -1:
                if previous_index != -1:
                    # try to find the word
                    candidate_index = previous_index + 1
                    if candidate_index < len(sentence_doc) and compare_tokens(sentence_doc[candidate_index], term_chunk_tokens[i]):
                        match_vector[i] = candidate_index
                    else:
                        break
                else:
                    go_backwards = True
            elif go_backwards:
                candidate_index = match_vector[i] - 1
                for i_backwards in range(i-1, -1, -1):
                    if candidate_index >= 0 and compare_tokens(sentence_doc[candidate_index], term_chunk_tokens[i_backwards]):
                        match_vector[i_backwards] = candidate_index
                        candidate_index = candidate_index -1
                    else:
                        break
            previous_index = match_vector[i]
    return match_vector


def initialize_match_vector(length):
    match_vector = []
    for i in range(0, length):
        match_vector.append(-1)
    return match_vector


def check_sequence_matching_vectors(vector_list):
    if isinstance(vector_list, list) and len(vector_list) > 0:
        first = True
        sequence = 0
        for vector in vector_list:
            for index in vector:
                if first:
                    first = False
                else:
                    if index <= sequence:
                        return False
                sequence = index
        return True
    else:
        return False


def fix_noun_chunks(document, all_propn):
    chunks = []
    token_indices = []
    # special case for proper names
    if all_propn:
        chunks.append(document[0:len(document)])
        return chunks

    # get chunks
    for chunk in document.noun_chunks:
        chunks.append(chunk)

    # save the indices of the tokens uof the chunks
    for chunk in chunks:
        for token in chunk:
            token_indices.append(token.i)

#     print("ORIGINAL chunks: !!")
#     for chunk in chunks:
#             print(chunk)

    # 1. fix noun chunks
    for i in range(len(chunks)):
        candidate_index = chunks[i][-1].i + 1
        start_chunk = chunks[i][0].i
        end_chunk = 0
        new_chunk = False
        while candidate_index < len(document):
            candidate_token = document[candidate_index]
            if (candidate_token.pos_ == "NOUN" or candidate_token.pos_ == "PROPN") and not candidate_index in token_indices:
                # print("can token: " + candidate_token.text, candidate_token.pos_, token_indices, candidate_index)
                new_chunk = True
                end_chunk = candidate_token.i
            else:
                break
            candidate_index += 1
        if new_chunk:
            new_span = document[start_chunk:end_chunk+1]
            chunks[i] = new_span

    # 2. add new single noun chunks
    for t in document:
        if t.pos_ == "NOUN" or t.pos_ == "PROPN":
            # check if token is part of a noun chunk
            is_part = False
            # print("checking: ", t)
            for chunk in chunks:
                for word in chunk:
                    if word.i == t.i:
                        is_part = True
                        break
            if not is_part:
                #print(" -- is not part", t)
                position_noun = t.i
                if len(chunks) > 0:
                    # check at beginning
                    if len(chunks[0]) > 0 and position_noun < chunks[0][0].i:
                        chunks.insert(0, document[position_noun: position_noun+1])
                    # check at the end
                    elif len(chunks[-1]) > 0 and position_noun > chunks[-1][-1].i:
                        chunks.append(document[position_noun: position_noun+1])
                    # check in between
                    else:
                        for i in range(0, len(chunks)-1):
                            if len(chunks[i]) > 0 and len(chunks[i+1]) > 0:
                                start_pos = chunks[i][-1].i
                                end_pos = chunks[i+1][0].i
                                if start_pos < position_noun < end_pos:
                                    chunks.insert(i+1, document[position_noun :  position_noun + 1])
                                    break
                else:
                    chunks.append(document[position_noun: position_noun+1])

#     print("FINAL chunks: !!", len(chunks))
#     for chunk in chunks:
#         print(chunk)

    return chunks

###########################################
def stem_sentence(doc):
    stem_sentence=[]
    for token in doc:
        stem_sentence.append(lancaster.stem(token.text))
        stem_sentence.append(" ")
    return "".join(stem_sentence)

def noun_extractor_main(lang, term_candidates, book_sentences, test_propn_p = True):

    # LOAD MODEL
    global nlp

    # CODE
    # PROCESS INDEX TERM
    terms_docs = []
    noun_chunks_terms = []
    tokens_chunks_terms = []
    propn_terms = []

    for term in term_candidates:
        # check if the term is a proper name
        all_propn = False
        if(test_propn_p):
            all_propn = all_propn_p(term)       
        propn_terms.append(all_propn)
        if not all_propn:
            term += text_to_noun_helper
        doc_term = nlp(term)
        terms_docs.append(doc_term)
        tokens_chunks_term = []
        noun_chunk_term = fix_noun_chunks(doc_term, all_propn)
        for chunk in noun_chunk_term:
            tokens_chunks = []
            for token in chunk:
                if token.pos_ in accepted_pos:
                    tokens_chunks.append(token)
            tokens_chunks_term.append(tokens_chunks)
        noun_chunks_terms.append(noun_chunk_term)
        tokens_chunks_terms.append(tokens_chunks_term)

    # PROCESS BOOK SENTENCES
    sentences_docs = []
    noun_chunks_sentences = []
    tokens_chunks_sentences = []
    for sentence in book_sentences:
        doc_sentence = nlp(sentence)
        all_propn = all_propn_p(sentence)
        sentences_docs.append(doc_sentence)
        tokens_chunks_sentence = []
        noun_chunk_sentence = fix_noun_chunks(doc_sentence, all_propn)
        for chunk in noun_chunk_sentence:
            tokens_chunks = []
            for token in chunk:
                if token.pos_ in accepted_pos:
                    tokens_chunks.append(token)
            tokens_chunks_sentence.append(tokens_chunks)
        noun_chunks_sentences.append(noun_chunk_sentence)
        tokens_chunks_sentences.append(tokens_chunks_sentence)

    # print("----DOCS----")
    # print(len(terms_docs), "-", terms_docs)
    # print(len(sentences_docs), "-", sentences_docs)
    #
#     print("----INDEX TERM----")
#     print(term_candidates)
#     print(noun_chunks_terms)
#     print(tokens_chunks_terms)
#     print(propn_terms)
#   
#     print("----BOOK SENTENCES---")
#     print(book_sentences)
#     print(noun_chunks_sentences)
#     print(tokens_chunks_sentences)
    #
    # print("----INDEX POS---")
    # for t in term_candidates:
    #     print("*new*")
    #     t += text_to_noun_helper
    #     doc = nlp(t)
    #     for token in doc:
    #         print("* ", token.text, token.lemma_, token.pos_, token.tag_, token.dep_,
    #               token.shape_, token.is_alpha, token.is_stop)
    
#     print("----INDEX NER---")
#     for t in term_candidates:
#         print("*new*")
#         #t += text_to_noun_helper
#         doc = nlp(t)
#         for ent in doc.ents:
#             print(ent.text, ent.start_char, ent.end_char, ent.label_)
    
#     print("----SENTENCES POS---")
#     for t in book_sentences:
#         #t += text_to_noun_helper
#         doc = nlp(t)
#         for token in doc:
#             print(token.text, token.lemma_, token.pos_, token.tag_, token.dep_,
#                   token.shape_, token.is_alpha, token.is_stop)

    # EVALUATION
    evaluation = []
    any_evaluation = False;
    for i in range(0, len(term_candidates)):
        # print("TERM CANDIDATE #", i)
        matching_sentences = []
        for s in range(0, len(book_sentences)):
            # print("SENTENCE #", s)
            len_noun_chunks_terms = len(noun_chunks_terms[i])
            matching_chunks = []
            matching_tokens_chunk_terms = []
            all_chunks = True
            for j in range(0, len_noun_chunks_terms):
                result = compare_chunks(tokens_chunks_terms[i][j], tokens_chunks_sentences[s], sentences_docs[s], propn_terms[i], initials(tokens_chunks_terms[i]))
#                 print("result", result)
                if result != False and result != []:
                    matching_chunks.append(result)
                    for token in tokens_chunks_terms[i][j]:
                        matching_tokens_chunk_terms.append(token.text)
                else:
                    all_chunks = False
                    if not propn_terms[i]:
                        break
            if propn_terms[i] and (len(matching_chunks) > 0):
                new_matching_chunks = []
                for match in matching_chunks:
                    new_matching_chunks.append(list(filter(lambda a: a != -1, match)))
                matching_chunks = new_matching_chunks
                matching_sentences.append(matching_chunks)
                any_evaluation = True
            elif all_chunks and check_sequence_matching_vectors(matching_chunks):
                matching_sentences.append(matching_chunks)
                any_evaluation = True
            else:
                matching_sentences.append(False)
        evaluation.append(matching_sentences)

#     print("----EVALUATION---")
#     print(evaluation)

    if not any_evaluation:
        # print("#####")
        # manually matching (word by word)
        for term_index in range(len(term_candidates)):
            term = term_candidates[term_index]
            #print("checking: ", term)
            term += text_to_noun_helper
            doc_term = nlp(term)
            list_tokens = []
            for token in doc_term:
                if token.pos_ not in rejected_pos:
                    list_tokens.append(token.lemma_)
            #print("list_tokens: ", list_tokens)
            if len(list_tokens) != 0:
                for sentence_index in range(len(book_sentences)):
                    sentence = book_sentences[sentence_index]
                    doc_sentence = nlp(sentence)
                    start_token = 0
                    positions = []
                    for i in range(len(doc_sentence)):
                        token = doc_sentence[i]
                        if token.pos_ not in rejected_pos and token.lemma_.lower() == list_tokens[start_token].lower():
                            positions.append(i)
                            start_token += 1
                            if start_token == len(list_tokens):
                                break
                    #print("len(positions)", len(positions), "len(list_tokens)", len(list_tokens))
                    if len(positions) == len(list_tokens):
                        evaluation[term_index][sentence_index] = [positions]

    # Construct JSON
    solutions = []
    for i in range(len(term_candidates)):
        solution = evaluation[i]
        solution_reached = False
        book_strings = []
        for j in range(len(solution)):
            vector = solution[j]
            if isinstance(vector, list):
                solution_reached = True;
                start_pos = vector[0][0]
                end_pos = vector[-1][-1]
                text = sentences_docs[j][start_pos:end_pos + 1].text
                if text not in book_strings:
                    book_strings.append(text)
        if solution_reached:
            dict = {}
            dict['correct'] = term_candidates[i]
            dict['pos'] = i
            chunks = []
            for s in noun_chunks_terms[i]:
                text = []
                for t in s:
                    if t.pos_ in accepted_pos:
                        text.append(t.text)
                chunks.append(" ".join(text))
            dict['noun_string'] = chunks
            dict['book_string'] = book_strings
            dict['proper_name'] = propn_terms[i]
            solutions.append(dict)
        else:
            # use matcher approach
            pattern = []
            doc = nlp(term_candidates[i])
            for token in doc:
                dict = {}
                if token.pos_ != 'PUNCT' and token.lemma_ != '-PRON-':
                    dict["LEMMA"] = token.lemma_.lower()
                    pattern.append(dict)
            if len(pattern) == 0:
                continue;
            matcher = Matcher(nlp.vocab)  
            matcher.add("token", None, pattern)
            for j in range(len(book_sentences)):
                doc_sent = nlp(book_sentences[j].lower())
                matches = matcher(doc_sent)
                for match_id, start, end in matches:
                    string_id = nlp.vocab.strings[match_id]  # Get string representation
                    span = doc_sent[start:end]  # The matched span
                    book_strings.append(span.text)
            if len(book_strings) > 0:
                dict = {}
                dict['correct'] = term_candidates[i]
                dict['pos'] = i
                dict['noun_string'] = []
                dict['book_string'] = book_strings
                dict['proper_name'] = propn_terms[i]
                solutions.append(dict)
            else:
                # use matcher approach with stemming
                pattern = []
                doc = nlp(term_candidates[i])
                for token in doc:
                    dict = {}
                    if token.pos_ != 'PUNCT' and token.lemma_ != '-PRON-':
                        dict["LOWER"] = lancaster.stem(token.lemma_.lower())
                        pattern.append(dict)
                if len(pattern) == 0:
                    continue;
                matcher = Matcher(nlp.vocab)  
                matcher.add("token", None, pattern)
                for j in range(len(book_sentences)):
                    doc_sent = nlp(book_sentences[j].lower())
                    stemmed_sentence = stem_sentence(doc_sent)
                    stemmed_doc_sent = nlp(stemmed_sentence)
                    matches = matcher(stemmed_doc_sent)
                    for match_id, start, end in matches:
                        string_id = nlp.vocab.strings[match_id]  # Get string representation
                        span = doc_sent[start:end]  # The matched span
                        book_strings.append(span.text)
                if len(book_strings) > 0:
                    dict = {}
                    dict['correct'] = term_candidates[i]
                    dict['pos'] = i
                    dict['noun_string'] = []
                    dict['book_string'] = book_strings
                    dict['proper_name'] = propn_terms[i]
                    solutions.append(dict)
                        
                
        # matcher = Matcher(nlp.vocab)
        # anything_flag = lambda text: bool(re.compile(r'.?').match(text))
        # IS_ANYTHING= nlp.vocab.add_flag(anything_flag)
        # pattern = [{'LEMMA': 'asymptotically'}, {'IS_ANYTHING':True}, {'LEMMA': 'unbiased'}]
        # matcher.add('HelloWorld', None, pattern)
        # print("pattern = ", pattern)
        # doc = nlp(u'asymptotically ( unbiased.')
        # matches = matcher(doc)
        # print(matches)
        # for match_id, start, end in matches:
        #     #string_id = nlp.vocab.strings[match_id]  # get string representation
        #     span = doc[start:end]  # the matched span
        #     print(match_id, start, end, span.text)
        
    return json.dumps(solutions, ensure_ascii=False)
    
def text_matcher(term, candidates, lang):
    # LOAD MODEL
    global nlp
    
    pattern = []
    doc = nlp(term.lower())
    optional = False;
    for token in doc:
        dict = {}
        if token.pos_ != 'PUNCT':
            dict["LEMMA"] = token.lemma_
            if optional:
                 dict["OP"] = '?'
            pattern.append(dict)
        elif token.pos_ == 'PUNCT' and (token.text == '(' or token.text == '[' or token.text == '{'):
             optional = True
        elif token.pos_ == 'PUNCT' and (token.text == ')' or token.text == ']' or token.text == '}'):
            optional = False
        elif token.pos_ == 'PUNCT':
            dict["LEMMA"] = token.lemma_
            dict["OP"] = '?'
    #print(pattern)
    matcher = Matcher(nlp.vocab)  
    matcher.add("token", None, pattern)
    best = 0;
    for candidate in candidates:
        doc = nlp(candidate.lower())
        size = 0;
        #length
        optional = False;
        for token in doc:
            if token.pos_ != 'PUNCT':
                if not optional:
                    size = size + 1
            elif token.pos_ == 'PUNCT' and (token.text == '(' or token.text == '[' or token.text == '{'):
                optional = True
            elif token.pos_ == 'PUNCT' and (token.text == ')' or token.text == ']' or token.text == '}'):
                optional = False
        #print("size", size)
        matches = matcher(doc)
        if(len(matches) > 0):
            for match_id, start, end in matches:
                size_match = end -start
                #print("size_match", size_match)
                score = size_match / size
                if score > best:
                    best = score
        
    return best 

# TESTING ONLY
# INPUT (TESTING ONLY)
# term_candidates_in = [u"distributed crawling"]
# book_sentences_in = [u"distributed crawler"]
# print(noun_extractor_main('en', term_candidates_in, book_sentences_in, False))

#                 u'mean for the bootstrap dataset: ¯x∗n−¯xn, where ¯x∗n= x 1+ x 2+···+ x n n . repeat steps 1 and 2 many times.',
#                 u"one of efron’s contributions was to point out how to combine the bootstrap with modern computational power."]

# term_candidates_in = [u"Type I error"]
# book_sentences_in = [u"Type I and type II errors"]

# term_candidates_in = [u"testing", u"hypotheses", u"type", u"I", u"error"]
# book_sentences_in = [u"P (T ≤ 61) .",  u"In other situations, the direction in which values of T provide stronger evidence against H0 may be to the right of the observed value t, in which case one would compute a right tail probability P (T ≥ t) .",  u"In both cases the tail probability expresses how likely it is to obtain a value of the test statistic T at least as extreme as the value t observed for the data.,  Such a probability is called a p-value.",  u"In a way, the size of the p-value reflects how much evidence the observed value t provides against H0.",  u"The smaller the p-value, the stronger evidence the observed value t bears against H0.",  u"The phrase “at least as extreme as the observed value t” refers to a particular direction, namely the direction in which values of T provide stronger evidence against H0 and in favor of H1.",  u"In our example, this was to the left of 61, and the p-value corresponding to 61 was P (T ≤ 61) = 0.00014.",  u"In this case it is clear what is meant by “at least as extreme as t” and which tail probability corresponds to the p-value.",  u"However, in some testing problems one can deviate from H0 in both directions.",  u"In such cases it may not be clear what values of T are at least as extreme as the observed value, and it may be unclear how the p-value should be computed.",  u"One approach to a solution in this case is to simply compute the one-tailed p-value that corresponds to the direction in which t deviates from H0.",  u"Quick exercise 25.3 Suppose that the Allied intelligence agencies had reported a production of 80 tanks, so that we would test H0 : N = 80 against H1 : N < 80.",  u"Compute the p-value corresponding to 61.",  u"Would you conclude H0 is false beyond reasonable doubt?"]

# term_candidates_in = [u"Caesar, Julius", u"Julius Caesar"]
# book_sentences_in = [u"Attacked by his godson, the gaucho dies “so that a scene [Julius Caesar’s murder at the hands of Brutus] can be played out again” (CF 307). ",
# u"we want to compute P. (x̄ )",u"test"]

#term_candidates_in = [u"constructed for boxplot"]
#book_sentences_in = [u"such an observation is called an outlier."]

# term_candidates_in = [u"correlated negatively"]
# book_sentences_in = [u"In case the covariance is negative, the opposite effect occurs; X and Y are  correlated negatively yes.", u"correlated negatively"]

# term_candidates = [u"for centered sample mean empirical bootstrap simulation is big"]
# book_sentences = [u"the centered sample mean is big"]

# print(noun_extractor_main('en', term_candidates_in, book_sentences_in, False))
# doc = nlp(u"distributed crawling, distributed crawler")
# print(stem_sentence(doc))
# print(text_matcher(u"joint probability density functions", [ u"joint Probability density function (pdf)"], "en"))

