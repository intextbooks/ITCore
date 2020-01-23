import spacy
import json
import re
from spacy.matcher import Matcher
from spacy import displacy

# CONSTANTS
accepted_pos = ["ADJ", "VERB", "NOUN", "PROPN"]
rejected_pos = ["AUX", "CONJ", "CCONJ", "DET", "INTJ", "SCONJ"]
text_to_noun_helper = " is big"


# INPUT (TESTING ONLY)
term_candidates_in = [u"empirical bootstrap simulation for centered sample mean", u"for centered sample mean empirical bootstrap simulation", "the centered sample mean"]
book_sentences_in = [u"the empirical bootstrap simulation is described for the centered sample mean, but clearly a similar simulation procedure can be formulated for any(normal- ized) sample statistic.",
                  u'mean for the bootstrap dataset: ¯x∗n−¯xn, where ¯x∗n= x 1+ x 2+···+ x n n . repeat steps 1 and 2 many times.',
                  u"one of efron’s contributions was to point out how to combine the bootstrap with modern computational power."]

term_candidates_in = [u"constructed for boxplot"]
book_sentences_in = [u"such an observation is called an outlier."]

# term_candidates = [u"bin of"]
# book_sentences = [u"These intervals are called bins and are denoted by B1, B2,...,Bm."]

# term_candidates = [u"for centered sample mean empirical bootstrap simulation is big"]
# book_sentences = [u"the centered sample mean is big"]

# ################FUNCTIONS#################


def compare_chunks(term_chunk_tokens, sentence_chunks_tokens, sentence_doc):

    # print("term doc: ", term_doc)
    # print("sentence  doc: ", sentence_doc)
    # print("comparing: ", term_chunk_tokens, " VS. ", sentence_chunks_tokens)
    for i in range(0, len(sentence_chunks_tokens)):
        match_vector = compare_chunk_tokens(term_chunk_tokens, sentence_chunks_tokens[i], sentence_doc)
        if is_match(match_vector):
            return match_vector
    return False


def compare_chunk_tokens(term_chunk_tokens, sentence_chunk_tokens, sentence_doc):
    matches = 0
    start_index = 0
    match_vector = initialize_match_vector(len(term_chunk_tokens))
    all_from_sentence = True
    for term_token_index in range(0, len(term_chunk_tokens)):
        all_from_sentence = True
        for sentence_token_index in range(start_index, len(sentence_chunk_tokens)):
            # print("C -> ", term_chunk_tokens[term_token_index].lemma_ , sentence_chunk_tokens[sentence_token_index].lemma)
            if term_chunk_tokens[term_token_index].lemma_ == sentence_chunk_tokens[sentence_token_index].lemma_:
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
    #     #print("MATCH!")
    # else:
    if all_from_sentence and len(sentence_chunk_tokens) > 0:
        #print("additional check")
        match_vector = additional_check(term_chunk_tokens, match_vector, sentence_doc)
    #     else:
    #        print("no match")
    # print("VECTOR (after) --> ", match_vector)
    return match_vector


def is_match(match_vector):
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
                    if candidate_index < len(sentence_doc) and sentence_doc[candidate_index].lemma_ == term_chunk_tokens[i].lemma_:
                        match_vector[i] = candidate_index
                    else:
                        break
                else:
                    go_backwards = True
            elif go_backwards:
                candidate_index = match_vector[i] - 1
                for i_backwards in range(i-1, -1, -1):
                    if candidate_index >= 0 and sentence_doc[candidate_index].lemma_ == term_chunk_tokens[i_backwards].lemma_:
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


def fix_noun_chunks(document):
    chunks = []
    # get chunks
    for chunk in document.noun_chunks:
        chunks.append(chunk)
    # print("ORIGINAL chunks: !!")
    # for chunk in chunks:
    #     print(chunk)
    # 1. fix noun chunks
    for i in range(len(chunks)):
        candidate_index = chunks[i][-1].i + 1
        start_chunk = chunks[i][0].i
        end_chunk = 0
        new_chunk = False
        while candidate_index < len(document):
            candidate_token = document[candidate_index]
            # print("can token: " + candidate_token.text, candidate_token.pos_)
            if candidate_token.pos_ == "NOUN":
                new_chunk = True
                end_chunk = candidate_token.i
            else:
                break
            candidate_index += 1
        if new_chunk:
            new_span = document[start_chunk : end_chunk+1]
            chunks[i] = new_span

    # 2. add new single noun chunks
    for t in document:
        if t.pos_ == "NOUN":
            # check if token is part of a noun chunk
            is_part = False
            # print("checking: ", t)
            for chunk in chunks:
                for word in chunk:
                    if word.text == t.text:
                        is_part = True
                        break
            if not is_part:
                # print(" -- is not part")
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

    # print("FINAL chunks: !!")
    # for chunk in chunks:
    #     print(chunk)

    return chunks

###########################################


def noun_extractor_main(lang, term_candidates, book_sentences):

    # LOAD MODEL
    nlp = spacy.load(lang)

    # CODE
    # PROCESS INDEX TERM
    terms_docs = []
    noun_chunks_terms = []
    tokens_chunks_terms = []
    for term in term_candidates:
        term += text_to_noun_helper
        doc_term = nlp(term)
        terms_docs.append(doc_term)
        tokens_chunks_term = []
        noun_chunk_term = fix_noun_chunks(doc_term)

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
        sentences_docs.append(doc_sentence)
        tokens_chunks_sentence = []
        noun_chunk_sentence = fix_noun_chunks(doc_sentence)
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
    # print("----INDEX TERM----")
    # print(term_candidates)
    # print(noun_chunks_terms)
    # print(tokens_chunks_terms)
    # #
    # print("----BOOK SENTENCES---")
    # print(book_sentences)
    # print(noun_chunks_sentences)
    # print(tokens_chunks_sentences)
    #
    # print("----INDEX POS---")
    # for t in term_candidates:
    #     t += text_to_noun_helper
    #     doc = nlp(t)
    #     for token in doc:
    #         print(token.text, token.lemma_, token.pos_, token.tag_, token.dep_,
    #               token.shape_, token.is_alpha, token.is_stop)
    #
    # print("----SENTENCES POS---")
    # for t in book_sentences:
    #     t += text_to_noun_helper
    #     doc = nlp(t)
    #     for token in doc:
    #         print(token.text, token.lemma_, token.pos_, token.tag_, token.dep_,
    #               token.shape_, token.is_alpha, token.is_stop)

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
            all_chunks = True
            for j in range(0, len_noun_chunks_terms):
                result = compare_chunks(tokens_chunks_terms[i][j], tokens_chunks_sentences[s], sentences_docs[s])
                if result != False:
                    matching_chunks.append(result)
                else:
                    all_chunks = False
                    break
            if all_chunks and check_sequence_matching_vectors(matching_chunks):
                matching_sentences.append(matching_chunks)
                any_evaluation = True;
            else:
                matching_sentences.append(False)
        evaluation.append(matching_sentences)

    # print(evaluation)

    if not any_evaluation:
        # print("#####")
        # manually matching
        for term_index in range(len(term_candidates)):
            term = term_candidates[term_index]
            # term += text_to_noun_helper
            doc_term = nlp(term)
            list_tokens = []
            for token in doc_term:
                if token.pos_ not in rejected_pos:
                    list_tokens.append(token.lemma_)
            if len(list_tokens) != 0:
                for sentence_index in range (len(book_sentences)):
                    sentence = book_sentences[sentence_index]
                    doc_sentence = nlp(sentence)
                    start_token = 0
                    positions = []
                    for i in range(len(doc_sentence)):
                        token = doc_sentence[i]
                        if token.pos_ not in rejected_pos and token.lemma_ == list_tokens[start_token]:
                            positions.append(i)
                            start_token += 1
                            if start_token == len(list_tokens):
                               break
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

# # TESTING ONLY
# print(noun_extractor_main('en', term_candidates_in, book_sentences_in))
