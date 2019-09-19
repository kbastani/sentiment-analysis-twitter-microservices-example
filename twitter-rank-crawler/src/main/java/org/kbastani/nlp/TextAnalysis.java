package org.kbastani.nlp;

import com.google.cloud.language.v1.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is modified from the examples provided by Google Cloud Natural Language.
 */
public class TextAnalysis {
    /**
     * Identifies entities in the string {@code text}.
     */
    public static List<Entity> analyzeEntitiesText(String text) {
        // [START language_entities_text]
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Document.Type.PLAIN_TEXT)
                    .build();
            AnalyzeEntitiesRequest request = AnalyzeEntitiesRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF16)
                    .build();

            AnalyzeEntitiesResponse response = language.analyzeEntities(request);

            return response.getEntitiesList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // [END language_entities_text]

        return new ArrayList<>();
    }

    /**
     * Identifies the sentiment in the string {@code text}.
     */
    public static Sentiment analyzeSentimentText(String text) {
        // [START language_sentiment_text]
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Document.Type.PLAIN_TEXT)
                    .build();
            AnalyzeSentimentResponse response = language.analyzeSentiment(doc);
            return response.getDocumentSentiment();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
        // [END language_sentiment_text]
    }

    /**
     * from the string {@code text}.
     */
    public static List<Token> analyzeSyntaxText(String text) throws Exception {
        // [START language_syntax_text]
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Document.Type.PLAIN_TEXT)
                    .build();
            AnalyzeSyntaxRequest request = AnalyzeSyntaxRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF16)
                    .build();
            // analyze the syntax in the given text
            AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
            // print the response
            for (Token token : response.getTokensList()) {
                System.out.printf("\tText: %s\n", token.getText().getContent());
                System.out.printf("\tBeginOffset: %d\n", token.getText().getBeginOffset());
                System.out.printf("Lemma: %s\n", token.getLemma());
                System.out.printf("PartOfSpeechTag: %s\n", token.getPartOfSpeech().getTag());
                System.out.printf("\tAspect: %s\n", token.getPartOfSpeech().getAspect());
                System.out.printf("\tCase: %s\n", token.getPartOfSpeech().getCase());
                System.out.printf("\tForm: %s\n", token.getPartOfSpeech().getForm());
                System.out.printf("\tGender: %s\n", token.getPartOfSpeech().getGender());
                System.out.printf("\tMood: %s\n", token.getPartOfSpeech().getMood());
                System.out.printf("\tNumber: %s\n", token.getPartOfSpeech().getNumber());
                System.out.printf("\tPerson: %s\n", token.getPartOfSpeech().getPerson());
                System.out.printf("\tProper: %s\n", token.getPartOfSpeech().getProper());
                System.out.printf("\tReciprocity: %s\n", token.getPartOfSpeech().getReciprocity());
                System.out.printf("\tTense: %s\n", token.getPartOfSpeech().getTense());
                System.out.printf("\tVoice: %s\n", token.getPartOfSpeech().getVoice());
                System.out.println("DependencyEdge");
                System.out.printf("\tHeadTokenIndex: %d\n", token.getDependencyEdge().getHeadTokenIndex());
                System.out.printf("\tLabel: %s\n\n", token.getDependencyEdge().getLabel());
            }
            return response.getTokensList();
        }
        // [END language_syntax_text]
    }

    /**
     * Detects categories in text using the Language Beta API.
     *
     * @return
     */
    public static List<ClassificationCategory> classifyText(String text) {

        List<ClassificationCategory> results = new ArrayList<>();
        // [START language_classify_text]
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            // set content to the text string
            Document doc = Document.newBuilder()
                    .setContent(text)
                    .setType(Document.Type.PLAIN_TEXT)
                    .build();
            ClassifyTextRequest request = ClassifyTextRequest.newBuilder()
                    .setDocument(doc)
                    .build();
            // detect categories in the given text
            ClassifyTextResponse response = language.classifyText(request);

            results = response.getCategoriesList();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return results;
        // [END language_classify_text]
    }

    /**
     * Detects the entity sentiments in the string {@code text} using the Language Beta API.
     *
     * @return
     */
    public static List<Entity> entitySentimentText(String text) {
        // [START language_entity_sentiment_text]
        // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
        try (LanguageServiceClient language = LanguageServiceClient.create()) {
            Document doc = Document.newBuilder()
                    .setContent(text).setType(Document.Type.PLAIN_TEXT).build();
            AnalyzeEntitySentimentRequest request = AnalyzeEntitySentimentRequest.newBuilder()
                    .setDocument(doc)
                    .setEncodingType(EncodingType.UTF16).build();
            // detect entity sentiments in the given string
            AnalyzeEntitySentimentResponse response = language.analyzeEntitySentiment(request);
            // Print the response
            return response.getEntitiesList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // [END language_entity_sentiment_text]
    }
}
