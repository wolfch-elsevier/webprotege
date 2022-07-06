package edu.stanford.bmir.protege.web.importer;

import static edu.stanford.bmir.protege.web.server.download.DownloadFormat.RDF_TURLE;
import static java.lang.System.out;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.semanticweb.owlapi.formats.RioTurtleDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntityProvider;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import edu.stanford.bmir.protege.web.server.change.AddAxiomChange;
import edu.stanford.bmir.protege.web.server.change.OntologyChange;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeRecordTranslatorImpl;
import edu.stanford.bmir.protege.web.server.index.impl.AnnotationAssertionAxiomsBySubjectIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.AnnotationAssertionAxiomsByValueIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.AnnotationAxiomsByIriReferenceIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.AxiomsByEntityReferenceIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.AxiomsByTypeIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.ClassAssertionAxiomsByIndividualIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.ClassHierarchyChildrenAxiomsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.DataPropertyAssertionAxiomsBySubjectIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.DeprecatedEntitiesByEntityIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.DifferentIndividualsAxiomsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.DisjointClassesAxiomsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.EquivalentClassesAxiomsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.IndexUpdater;
import edu.stanford.bmir.protege.web.server.index.impl.ObjectPropertyAssertionAxiomsBySubjectIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.OntologyAnnotationsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.ProjectOntologiesIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.SameIndividualAxiomsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.SubAnnotationPropertyAxiomsBySuperPropertyIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.SubClassOfAxiomsBySubClassIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.UpdatableIndex;
import edu.stanford.bmir.protege.web.server.inject.ChangeHistoryFileFactory;
import edu.stanford.bmir.protege.web.server.inject.project.ProjectDirectoryFactory;
import edu.stanford.bmir.protege.web.server.owlapi.WebProtegeOWLManager;
import edu.stanford.bmir.protege.web.server.revision.Revision;
import edu.stanford.bmir.protege.web.server.revision.RevisionManager;
import edu.stanford.bmir.protege.web.server.revision.RevisionManagerImpl;
import edu.stanford.bmir.protege.web.server.revision.RevisionStore;
import edu.stanford.bmir.protege.web.server.revision.RevisionStoreImpl;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.revision.RevisionNumber;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class WPImporter {
    static final String ONTOLOGY_IRI_TEMPLATE = "http://webprotege.stanford.edu/projects/%s/ontologies/temp";
    static final Logger logger = LoggerFactory.getLogger(WPImporter.class);
    static final Pattern TTLFILENAME = Pattern.compile("\\w+_(\\d+)\\.ttl");
    static final OWLOntologyID DEFAULT_OOID = new OWLOntologyID(
	    Optional.of(IRI.create("urn:webprotege:ontology:a8b3604e-fdfe-4310-a894-2dae3d6ff056")),
	    Optional.<IRI>absent());
    final UserId userId;
    final ProjectId projectId;
    final Path baseDataDir;

    protected AtomicInteger revisionSubmissions = new AtomicInteger();

    public WPImporter(String userId, String projectId, Path baseDataDir) {
	this.userId = UserId.valueOf(userId);
	this.projectId = ProjectId.get(projectId);
	this.baseDataDir = baseDataDir;
	System.setProperty("webprotege.data.directory", baseDataDir.toString());
    }

    public static void main(String[] args) throws Exception {
	long started = System.currentTimeMillis();

	WPImporter wpp = new WPImporter("stanAdmin", "e4e4ac44-9906-484f-8be4-73f9639e11fc", Paths.get("/srv/tmp"));

	// ProjectId projectId = ProjectId.get("3706db44-75f9-4b26-b75e-ba50986259b6");
	// ProjectId projectId = ProjectId.get("ed0215fa-db5a-4e2f-bc9d-c80c17506ab5");
	ProjectId projectId = ProjectId.get("749507e7-504a-40fa-bc3c-0665c27dae65");
	wpp.logStoredRevsions(projectId, Paths.get("/srv/wpdata"));
	// RevisionStoreImpl store = wpp.open_revision_store(projectId);

	// InputStream inputStream = Files.newInputStream(Paths.get("..",
	// "output_sample.ttl"));

	// Set<OWLAxiom> axioms =
	// wpp.loadAxioms(ProjectId.get("3706db44-75f9-4b26-b75e-ba50986259b6"),
	// new RioTurtleDocumentFormat(), RDF_TURLE.getMimeType(), inputStream);
	// out.println(axioms.size());

	// wpp.loadTriples(projectId, inputStream);

	// List<String> hgraphKinds = Arrays.asList(new String[]{"model", "hierarchy"});

	// List<String> hgraphKinds = Arrays.asList(HGRAPH_KINDS);
	// wpp.loadCachedTriples(projectId,
	// Paths.get("/Users/ma-wolf2/src/webprotege/data/cache"), hgraphKinds);

	long runtimeMillis = System.currentTimeMillis() - started;
	logger.info("All triples loaded in {}", DurationFormatUtils.formatDuration(runtimeMillis, "H:mm:ss", true));

	// wpp.logStoredRevsions(projectId);

	/*-
	IndexUpdater indexUpdater = wpp.createIndexer(projectId);
	indexUpdater.buildIndexes();	
	wpp.shutdownAndAwaitTermination(indexUpdater.getIndexUpdaterService());
	*/
    }

    /**
     * Load triples from <code>inputStream</code> where <code>inputStream</code> is
     * a source in TURTLE format. A single call to this function represents a
     * "revision" in the Webprotege revision history, so each call requires a unique
     * <code>revisionNumber</code> (usually an ascending count).
     * 
     * @param projectId
     * @param inputStream
     * @param revisionMessage
     * @param revisionNumber
     * @param revisionStore
     */
    public void loadTriples(ProjectId projectId, InputStream inputStream, String revisionMessage, long revisionNumber,
	    RevisionStore revisionStore) {
	Set<OWLAxiom> axioms = loadAxioms(projectId, new RioTurtleDocumentFormat(), RDF_TURLE.getMimeType(),
		inputStream, revisionMessage);

	List<OntologyChange> changes = new ArrayList<>();

	for (OWLAxiom axiom : axioms) {
	    OntologyChange c = AddAxiomChange.of(DEFAULT_OOID, axiom);
	    changes.add(c);
	}

	Revision revision = new Revision(UserId.getUserId("stanAdmin"),
		RevisionNumber.getRevisionNumber(revisionNumber), ImmutableList.copyOf(changes), new Date().getTime(),
		revisionMessage);
	revisionStore.addRevision(revision);
	int revisionsOutstanding = this.revisionSubmissions.incrementAndGet();
	logger.info("Submitted {} for persisting, {} submissions outstanding...", revisionMessage,
		revisionsOutstanding);
    }

    /**
     * Writes to the log each revision message and number.
     * 
     * @param projectId
     * @param baseDataDir
     */
    void logStoredRevsions(ProjectId projectId, Path baseDataDir) {
	RevisionStoreImpl revisionStore = openRevisionStore(projectId, baseDataDir);
	revisionStore.load();
	RevisionNumber revisionNumber = revisionStore.getCurrentRevisionNumber();
	out.println(revisionNumber);
	for (Revision revision : revisionStore.getRevisions())
	    logger.info(revision.toString());
	revisionStore.dispose();
    }

    RevisionStoreImpl openRevisionStore(ProjectId projectId, Path baseDataDir) {
	RevisionStoreImpl store;
	OWLDataFactoryImpl dataFactory = new OWLDataFactoryImpl();
	OntologyChangeRecordTranslatorImpl changeRecordTranslator = new OntologyChangeRecordTranslatorImpl();
	ChangeHistoryFileFactory changeHistoryFileFactory = new ChangeHistoryFileFactory(
		new ProjectDirectoryFactory(baseDataDir.toFile()));
	store = new RevisionStoreImpl(projectId, changeHistoryFileFactory, dataFactory, changeRecordTranslator);

	return store;
    }

    /**
     * Load triples from the indicated <code>inputStream</code> according to
     * <code>documentFormat</code> and <code>mimeType</code>.
     * 
     * @param projectId
     * @param documentFormat
     * @param mimeType
     * @param inputStream
     * @return
     */
    public Set<OWLAxiom> loadAxioms(@Nonnull ProjectId projectId, @Nonnull OWLDocumentFormat documentFormat,
	    @Nonnull String mimeType, @Nonnull InputStream inputStream, @Nonnull String revisionMessage) {
	Stopwatch stopwatch = Stopwatch.createStarted();
	logger.info("{} Processing axioms (format: {} mime-type: {})", projectId, documentFormat.getKey(), mimeType);
	try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
	    OWLOntologyManager manager = WebProtegeOWLManager.createOWLOntologyManager();
	    IRI tempDocumentIri = IRI.create(String.format(ONTOLOGY_IRI_TEMPLATE, projectId.getId()));
	    OWLOntologyDocumentSource source = new StreamDocumentSource(bufferedInputStream, tempDocumentIri,
		    documentFormat, mimeType);
	    OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
	    configuration = configuration.setReportStackTraces(false);
	    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(source, configuration);
	    Set<OWLAxiom> axioms = ontology.getAxioms(Imports.INCLUDED);
	    logger.info("{} Successfully parsed {} axioms for {}", projectId, axioms.size(), revisionMessage);
	    return axioms;
	} catch (Exception e) {
	    logger.error("{} An error occurred while parsing axioms: {} for {}", projectId, e.getMessage(),
		    revisionMessage);
	    throw new RuntimeException(e);
	} finally {
	    stopwatch.stop();
	    logger.info("{} Finished processing axioms in {} ms for {}", projectId, stopwatch.elapsed(MILLISECONDS),
		    revisionMessage);
	}
    }

    IndexUpdater createIndexer(ProjectId projectId, Path wpDataDir) {

	// DataDirectoryProvider ddp = new DataDirectoryProvider(new
	// WebProtegeProperties(new Properties()));
	Set<UpdatableIndex> indexes = new LinkedHashSet<>();

	RevisionStoreImpl revisionStore = openRevisionStore(projectId, wpDataDir);
	revisionStore.load();
	RevisionManager revisionManager = new RevisionManagerImpl(revisionStore);
	ProjectOntologiesIndexImpl projectOntologiesIndex = new ProjectOntologiesIndexImpl();
	projectOntologiesIndex.init(revisionManager);

	UpdatableIndex index = new ClassHierarchyChildrenAxiomsIndexImpl(projectOntologiesIndex);
	indexes.add(index);
	index = new DeprecatedEntitiesByEntityIndexImpl(projectOntologiesIndex);
	indexes.add(index);
	index = new AnnotationAssertionAxiomsBySubjectIndexImpl();
	indexes.add(index);
	index = new AnnotationAxiomsByIriReferenceIndexImpl();
	indexes.add(index);

	OWLEntityProvider owlEntityProvider = new OWLDataFactoryImpl();
	index = new AxiomsByEntityReferenceIndexImpl(owlEntityProvider);
	indexes.add(index);

	index = new AnnotationAssertionAxiomsByValueIndexImpl();
	indexes.add(index);
	index = new AxiomsByTypeIndexImpl();
	indexes.add(index);
	index = new ClassAssertionAxiomsByIndividualIndexImpl();
	indexes.add(index);
	index = new DataPropertyAssertionAxiomsBySubjectIndexImpl();
	indexes.add(index);
	index = new DifferentIndividualsAxiomsIndexImpl();
	indexes.add(index);
	index = new DisjointClassesAxiomsIndexImpl();
	indexes.add(index);
	index = new ObjectPropertyAssertionAxiomsBySubjectIndexImpl();
	indexes.add(index);
	index = new OntologyAnnotationsIndexImpl();
	indexes.add(index);
	index = new SameIndividualAxiomsIndexImpl();
	indexes.add(index);
	index = new SubAnnotationPropertyAxiomsBySuperPropertyIndexImpl();
	indexes.add(index);
	index = new SubClassOfAxiomsBySubClassIndexImpl();
	indexes.add(index);
	index = new EquivalentClassesAxiomsIndexImpl();
	indexes.add(index);
	// index = new ProjectOntologiesIndexImpl();
	indexes.add(projectOntologiesIndex);

	int coreCount = Runtime.getRuntime().availableProcessors();

	logger.info("Available cores: {}", coreCount);

	var executor = Executors.newFixedThreadPool(coreCount, r -> {
	    Thread thread = Executors.defaultThreadFactory().newThread(r);
	    thread.setName(thread.getName().replace("thread", "Index-Updater"));
	    return thread;
	});

	IndexUpdater indexUpdater = new IndexUpdater(revisionManager, indexes, executor, projectId);

	return indexUpdater;
    }

    /*-    
    LuceneIndexWriter createIndexWriter(ProjectId projecyId) {
    MultiLingualShortFormDictionaryLuceneImpl mlsfd = new MultiLingualShortFormDictionaryLuceneImpl(null);
        LuceneEntityDocumentTranslator luceneEntityDocumentTranslator;
        LuceneIndexWriter iw = new LuceneIndexWriterImpl(
            projectId,
            luceneDirectory,
            luceneEntityDocumentTranslator,
            projectSignatureIndex,
            entitiesInProjectSignatureIndex,
            indexWriter,
            searcherManager,
            builtInOwlEntitiesIndex);
    
        return iw;
    }*/

    void shutdownAndAwaitTermination(ExecutorService pool) {
	pool.shutdown(); // Disable new tasks from being submitted
	try {
	    // Wait a while for existing tasks to terminate
	    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
		pool.shutdownNow(); // Cancel currently executing tasks
		// Wait a while for tasks to respond to being cancelled
		if (!pool.awaitTermination(60, TimeUnit.SECONDS))
		    logger.error("Pool did not terminate");
	    }
	} catch (InterruptedException ie) {
	    // (Re-)Cancel if current thread also interrupted
	    pool.shutdownNow();
	    // Preserve interrupt status
	    Thread.currentThread().interrupt();
	}
    }
    /*-
    static class ChangeHistoryFileFactory2 extends ChangeHistoryFileFactory {
    
    public ChangeHistoryFileFactory2(Path dataDirectoryPath, Path changeFilePath) {
        super(dataDirectoryPath, changeFilePath);
    }
    
    @Override
    public File getChangeHistoryFile(@Nonnull ProjectId projectId) {
        return new File(this.projectDirectoryFactory.getProjectDirectory(projectId),
    	    this.changeFilePath.toString());
    }
    }*/

    public AtomicInteger getRevisionSubmissions() {
	return revisionSubmissions;
    }

}
