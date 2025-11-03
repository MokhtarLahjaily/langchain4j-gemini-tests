package ma.emsi.lahjaily;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.time.Duration;

public class Test4 {

    // Assistant conversationnel
    interface Assistant {
        // Prend un message de l'utilisateur et retourne une réponse du LLM.
        String chat(String userMessage);
    }

    public static void main(String[] args) {


        String apiKey = System.getenv("GEMINI_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Erreur : La variable d'environnement GEMINI_KEY n'est pas définie.");
            return;
        }

        // Configuration et création du modèle de chat (LLM)
        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.3)
                .timeout(Duration.ofSeconds(60))
                .logRequestsAndResponses(true) // Activer les logs pour le débogage
                .build();

        // Début de la configuration RAG (Retrieval-Augmented Generation)
        // L'objectif est de donner au LLM un "contexte" (le fichier infos.txt)


        String nomDocument = "infos.txt";
        Document document = FileSystemDocumentLoader.loadDocument(nomDocument);

        // Création du modèle d'embedding (pour vectoriser le texte)
        EmbeddingModel embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-004")
                .build();

        // Création de la base de données vectorielle en mémoire
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // "Ingestion" du document :
        // Le document est segmenté, vectorisé (via embeddingModel) et stocké dans embeddingStore
        EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(document);


        // Création de l'assistant conversationnel, avec une mémoire. (via AiServices)
        // L'implémentation de Assistant est faite par LangChain4j.
        // L'assistant gardera en mémoire les 10 derniers messages.
        // La base vectorielle en mémoire est utilisée pour retrouver les embeddings.
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .contentRetriever(EmbeddingStoreContentRetriever.builder()
                        .embeddingStore(embeddingStore)
                        .embeddingModel(embeddingModel)
                        .maxResults(3)
                        .build())
                .build();


        // String question = "Pierre appelle son chat. Qu'est-ce qu'il pourrait dire ?";
        // String question = "Comment s'appelle le chat de Pierre ?";
         String question = "Quelle est la capitale de la France ?";

        String answer = assistant.chat(question);

        System.out.println("==================================================");
        System.out.println("Question : " + question);
        System.out.println("Réponse : " + answer);
        System.out.println("==================================================");
    }
}