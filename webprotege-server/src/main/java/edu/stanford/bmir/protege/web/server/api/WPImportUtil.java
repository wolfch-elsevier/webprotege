package edu.stanford.bmir.protege.web.server.api;

import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.inject.Provider;

import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.semanticweb.owlapi.model.OWLDataFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import edu.stanford.bmir.protege.web.server.crud.EntityIriPrefixResolver;
import edu.stanford.bmir.protege.web.server.index.EntitiesInProjectSignatureByIriIndex;
import edu.stanford.bmir.protege.web.server.index.ProjectOntologiesIndex;
import edu.stanford.bmir.protege.web.server.inject.OverridableFileFactory;
import edu.stanford.bmir.protege.web.server.owlapi.NonCachingDataFactory;
import edu.stanford.bmir.protege.web.server.project.BuiltInPrefixDeclarations;
import edu.stanford.bmir.protege.web.server.project.BuiltInPrefixDeclarationsLoader;
import edu.stanford.bmir.protege.web.server.project.ProjectDetailsRepository;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class WPImportUtil {
  public static MongoDatabase connectToMongoDb(String mongodbUri) {
    // String uri = "mongodb://user:pass@sample.host:27017/?maxPoolSize=20&w=majority";
    try (com.mongodb.client.MongoClient mongoClient = MongoClients.create(mongodbUri)) {
      MongoDatabase database = mongoClient.getDatabase("admin");
      try {
        Bson command = new BsonDocument("ping", new BsonInt64(1));
        Document commandResult = database.runCommand(command);
        // System.out.println("Connected successfully to server.");
      }
      catch (MongoException me) {
        String msg = String.format("Can't connect to '%s'", mongodbUri);
        throw new RuntimeException(msg, me);
      }
      return database;
    }
  }

  /**
   * Expects to find file named "built-in-prefixes.csv" in dataDir.
   * @param dataDir
   * @return
   */
  public static BuiltInPrefixDeclarations getBuiltInPrefixDeclarations(Path dataDir) {
    OverridableFileFactory fileFactory = new OverridableFileFactory(dataDir.toFile());
    BuiltInPrefixDeclarationsLoader manager = new BuiltInPrefixDeclarationsLoader(fileFactory);
    BuiltInPrefixDeclarations decls = manager.getBuiltInPrefixDeclarations();
    return decls;
  }

  static class ProjectIdProvider
    implements Provider<ProjectId> {
    ProjectId projectId;

    public ProjectIdProvider(ProjectId projectId) {
      this.projectId = projectId;
    }

    @Override
    public ProjectId get() {
      return projectId;
    }

  }

  static class DatabaseProvider
    implements Provider<MongoDatabase> {
    protected MongoDatabase database;

    public DatabaseProvider(String mongodbUri) {
      database = connectToMongoDb(mongodbUri);
    }

    @Override
    public MongoDatabase get() {
      return database;
    }

  }

  static class ProjectDetailsRepositoryProvider
    implements Provider<ProjectDetailsRepository> {
    Provider<MongoDatabase> databaseProvider;
    Provider<ObjectMapper> objectMapperProvider;

    public ProjectDetailsRepositoryProvider(Provider<MongoDatabase> databaseProvider,
      Provider<ObjectMapper> objectMapperProvider) {
      this.databaseProvider = databaseProvider;
      this.objectMapperProvider = objectMapperProvider;
    }

    @Override
    public ProjectDetailsRepository get() {
      return new ProjectDetailsRepository(databaseProvider.get(), objectMapperProvider.get());
    }

  }

  static class OWLDataFactoryProvider
    implements Provider<OWLDataFactory> {
    OWLDataFactory owlDataFactory;

    @Override
    public OWLDataFactory get() {
      if (owlDataFactory == null)
        owlDataFactory = new NonCachingDataFactory(new OWLDataFactoryImpl());
      return owlDataFactory;
    }

  }

  static class EntitiesInProjectSignatureByIriIndexProvider
    implements Provider<EntitiesInProjectSignatureByIriIndex> {
    EntitiesInProjectSignatureByIriIndex instance;

    public EntitiesInProjectSignatureByIriIndexProvider(
      @Nonnull ProjectOntologiesIndex projectOntologiesIndex) {
      EntityIriPrefixResolver r;
    }

    @Override
    public EntitiesInProjectSignatureByIriIndex get() {
      // TODO Auto-generated method stub
      return null;
    }

  }
}
