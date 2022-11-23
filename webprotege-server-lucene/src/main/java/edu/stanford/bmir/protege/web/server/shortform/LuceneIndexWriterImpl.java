package edu.stanford.bmir.protege.web.server.shortform;

import com.google.common.base.Stopwatch;
import edu.stanford.bmir.protege.web.server.index.BuiltInOwlEntitiesIndex;
import edu.stanford.bmir.protege.web.server.index.EntitiesInProjectSignatureIndex;
import edu.stanford.bmir.protege.web.server.index.ProjectSignatureIndex;
import edu.stanford.bmir.protege.web.server.search.EntitySearchFilterIndexesManager;
import edu.stanford.bmir.protege.web.shared.HasDispose;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Provider;

import static java.lang.System.out;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2020-07-07
 */
public class LuceneIndexWriterImpl implements LuceneIndexWriter, HasDispose, EntitySearchFilterIndexesManager {

    private static final Logger logger = LoggerFactory.getLogger(LuceneIndexWriterImpl.class);

    @Nonnull
    private final ProjectId projectId;

    @Nonnull
    private final Directory luceneDirectory;

    @Nonnull
    private final Provider<LuceneEntityDocumentTranslator> luceneEntityDocumentTranslator;

    @Nonnull
    private final ProjectSignatureIndex projectSignatureIndex;

    @Nonnull
    private final EntitiesInProjectSignatureIndex entitiesInProjectSignatureIndex;

    @Nonnull
    private final IndexWriter indexWriter;

    @Nonnull
    private final SearcherManager searcherManager;

    @Nonnull
    private BuiltInOwlEntitiesIndex builtInOwlEntitiesIndex;


    @Inject
    public LuceneIndexWriterImpl(@Nonnull ProjectId projectId,
                                 @Nonnull Directory luceneDirectory,
                                 @Nonnull Provider<LuceneEntityDocumentTranslator> luceneEntityDocumentTranslator,
                                 @Nonnull ProjectSignatureIndex projectSignatureIndex,
                                 @Nonnull EntitiesInProjectSignatureIndex entitiesInProjectSignatureIndex,
                                 @Nonnull IndexWriter indexWriter,
                                 @Nonnull SearcherManager searcherManager,
                                 @Nonnull BuiltInOwlEntitiesIndex builtInOwlEntitiesIndex) {
        this.projectId = projectId;
        this.luceneDirectory = luceneDirectory;
        this.luceneEntityDocumentTranslator = luceneEntityDocumentTranslator;
        this.projectSignatureIndex = checkNotNull(projectSignatureIndex);
        this.entitiesInProjectSignatureIndex = entitiesInProjectSignatureIndex;
        this.indexWriter = indexWriter;
        this.searcherManager = searcherManager;
        this.builtInOwlEntitiesIndex = checkNotNull(builtInOwlEntitiesIndex);

        logger.info("=========== LuceneIndexWriterImpl ===========");
        List<StackFrame> stack = StackWalker.getInstance().walk(s ->
        s.limit(25).collect(Collectors.toList()));
        logger.info("Directory: {}", luceneDirectory.getClass().getName());
        logger.info("LuceneEntityDocumentTranslator: {}", luceneEntityDocumentTranslator.getClass().getName());
        logger.info("ProjectSignatureIndex: {}", projectSignatureIndex.getClass().getName());
        logger.info("EntitiesInProjectSignatureIndex: {}", entitiesInProjectSignatureIndex.getClass().getName());
        logger.info("IndexWriter: {}", indexWriter.getClass().getName());
        logger.info("SearcherManager: {}", searcherManager.getClass().getName());
        logger.info("BuiltInOwlEntitiesIndex: {}", builtInOwlEntitiesIndex.getClass().getName());

	stack.forEach(s -> {
	    logger.info(s.toString());
	});
    }

    @Override
    public void updateEntitySearchFilterIndexes() {
        try {
            rebuildIndex();
        } catch (IOException e) {
            logger.error("An error occurred while rebuilding the entity search filter index", e);
        }
    }

    @Override
    public void rebuildIndex() throws IOException {
        indexWriter.deleteAll();
        buildAndWriteIndex();
    }

    @Override
    public void writeIndex() throws IOException {

        if(DirectoryReader.indexExists(luceneDirectory)) {
            logger.info("{} Lucene index already exists", projectId);
            return;
        }
        buildAndWriteIndex();
    }

    private void buildAndWriteIndex() throws IOException {
        logger.info("{} Building lucene index", projectId);
        var stopwatch = Stopwatch.createStarted();

        var docTranslator = luceneEntityDocumentTranslator.get();
        projectSignatureIndex.getSignature()
                             .peek(this::logProgress)
                             .map(docTranslator::getLuceneDocument)
                             .forEach(this::addDocumentToIndex);
        builtInOwlEntitiesIndex.getBuiltInEntities()
                               .filter(entity -> !entitiesInProjectSignatureIndex.containsEntityInSignature(entity))
                               .map(docTranslator::getLuceneDocument)
                               .forEach(this::addDocumentToIndex);
        indexWriter.commit();
        searcherManager.maybeRefreshBlocking();
        logger.info("{} Built lucene based dictionary in {} ms", projectId, stopwatch.elapsed().toMillis());
    }

    private int counter = 0;

    private void logProgress(OWLEntity entity) {
        counter++;
        if(counter % 10_000 == 0) {
            logger.info("    {} Lucene index writer: Added {} entities to index", projectId, counter);
        }
    }

    public void addDocumentToIndex(Document doc) {
        try {
            indexWriter.addDocument(doc);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void dispose() {
        try {
            indexWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
