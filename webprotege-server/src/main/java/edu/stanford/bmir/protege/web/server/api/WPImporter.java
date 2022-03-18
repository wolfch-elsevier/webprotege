package edu.stanford.bmir.protege.web.server.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.formats.RioTurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;

import edu.stanford.bmir.protege.web.server.api.axioms.PostedAxiomsLoadResponse;
import edu.stanford.bmir.protege.web.server.api.axioms.PostedAxiomsLoader;
import edu.stanford.bmir.protege.web.server.change.AddAxiomChange;
import edu.stanford.bmir.protege.web.server.change.FixedChangeListGenerator;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeList;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeRecordTranslatorImpl;
import edu.stanford.bmir.protege.web.server.index.OntologyAnnotationsIndex;
import edu.stanford.bmir.protege.web.server.index.OntologyAxiomsIndex;
import edu.stanford.bmir.protege.web.server.index.RootIndex;
import edu.stanford.bmir.protege.web.server.index.impl.AxiomsByTypeIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.OntologyAnnotationsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.OntologyAxiomsIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.ProjectOntologiesIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.RootIndexImpl;
import edu.stanford.bmir.protege.web.server.inject.ChangeHistoryFileFactory;
import edu.stanford.bmir.protege.web.server.project.DefaultOntologyIdManager;
import edu.stanford.bmir.protege.web.server.project.DefaultOntologyIdManagerImpl;
import edu.stanford.bmir.protege.web.server.project.chg.ChangeManager;
import edu.stanford.bmir.protege.web.server.revision.RevisionManager;
import edu.stanford.bmir.protege.web.server.revision.RevisionManagerImpl;
import edu.stanford.bmir.protege.web.server.revision.RevisionStore;
import edu.stanford.bmir.protege.web.server.revision.RevisionStoreImpl;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class WPImporter {

  public static void main(String[] args)
    throws Exception {
    ProjectId projectId = ProjectId.get("ed0215fa-db5a-4e2f-bc9d-c80c17506ab5");
    Path ttlFilePath = Paths.get("/Users/ma-wolf2/Downloads/output_sample.ttl");
    Path baseDataDir = Paths.get("/Users/ma-wolf2/Downloads");
    Path changesFile = Paths.get("change-data.binary");
    UserId userId = UserId.valueOf("wolf2");
  }

  int persistAxioms(ProjectId projectId, UserId userId, Stream<OWLAxiom> axioms,
    String commitMessage, Path baseDataDir, Path changesFile) {
    var builder = OntologyChangeList.<String> builder();
    ProjectOntologiesIndexImpl poi = new ProjectOntologiesIndexImpl();
    RevisionStore revisionStore = createRevisionStore(projectId, baseDataDir, changesFile);
    RevisionManager revisionMgr = new RevisionManagerImpl(revisionStore);
    poi.init(revisionMgr);

    DefaultOntologyIdManager defaultOntologyIdManager = new DefaultOntologyIdManagerImpl(null);

    var ontId = defaultOntologyIdManager.getDefaultOntologyId();
    axioms.forEach(ax -> builder.add(AddAxiomChange.of(ontId, ax)));
    var changeList = builder.build(commitMessage);
    var changeListGenerator = new FixedChangeListGenerator<>(changeList.getChanges(), "",
      commitMessage);

    ChangeManager changeManager = new ChangeManager(null, null, null, null, null, null, null, null,
      null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
      defaultOntologyIdManager, null, null);

    var result = changeManager.applyChanges(userId, changeListGenerator);
    int addedAxiomsCount = result.getChangeList().size();
    return addedAxiomsCount;
  }

  PostedAxiomsLoadResponse loadAxioms(ProjectId projectId, Path ttlFilePath)
    throws IOException {
    OWLDocumentFormat documentFormat = new RioTurtleDocumentFormat();
    String mimeType = "text/turtle";
    PostedAxiomsLoader axiomsLoader = new PostedAxiomsLoader(projectId, documentFormat, mimeType);
    InputStream inputStream = Files.newInputStream(ttlFilePath);
    PostedAxiomsLoadResponse loadResponse = axiomsLoader.loadAxioms(inputStream);
    return loadResponse;
  }

  RevisionStore createRevisionStore(ProjectId projectId, Path baseDataDir, Path changesFile) {
    RevisionStoreImpl store;
    OWLDataFactoryImpl dataFactory = new OWLDataFactoryImpl();
    OntologyChangeRecordTranslatorImpl changeRecordTranslator = new OntologyChangeRecordTranslatorImpl();

    ChangeHistoryFileFactory changeHistoryFileFactory = new ChangeHistoryFileFactory2(baseDataDir,
      changesFile);

    store = new RevisionStoreImpl(projectId, changeHistoryFileFactory, dataFactory,
      changeRecordTranslator);

    store.load();

    return store;

    // store.load();
    // RevisionNumber revisionNumber = store.getCurrentRevisionNumber();
    // out.println(revisionNumber);
    // for (Revision revision : store.getRevisions())
    // out.println(revision);
    // store.dispose();

  }

  RootIndex createRootIndex() {
    AxiomsByTypeIndexImpl axiomsByTypeIndex = new AxiomsByTypeIndexImpl();
    OntologyAxiomsIndex ontologyAxiomsIndex = new OntologyAxiomsIndexImpl(axiomsByTypeIndex);
    OntologyAnnotationsIndex ontologyAnnotationsIndex = new OntologyAnnotationsIndexImpl();
    RootIndex rootIndex = new RootIndexImpl(ontologyAxiomsIndex, ontologyAnnotationsIndex);
    return rootIndex;
  }

  static class ChangeHistoryFileFactory2
    extends ChangeHistoryFileFactory {

    public ChangeHistoryFileFactory2(Path dataDirectoryPath, Path changeFilePath) {
      super(dataDirectoryPath, changeFilePath);
    }

    @Override
    public File getChangeHistoryFile(@Nonnull ProjectId projectId) {
      return new File(this.projectDirectoryFactory.getProjectDirectory(projectId),
        this.changeFilePath.toString());
    }
  }

}
