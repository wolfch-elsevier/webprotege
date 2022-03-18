package edu.stanford.bmir.protege.web.server.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;
import javax.inject.Provider;

import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoDatabase;

import edu.stanford.bmir.protege.web.server.change.ChangeApplicationResult;
import edu.stanford.bmir.protege.web.server.change.ChangeGenerationContext;
import edu.stanford.bmir.protege.web.server.change.ChangeListGenerator;
import edu.stanford.bmir.protege.web.server.change.HasApplyChanges;
import edu.stanford.bmir.protege.web.server.change.OntologyChange;
import edu.stanford.bmir.protege.web.server.change.OntologyChangeList;
import edu.stanford.bmir.protege.web.server.crud.ChangeSetEntityCrudSession;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudContext;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudContextFactory;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudKitHandler;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudKitPlugin;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudKitPluginManager;
import edu.stanford.bmir.protege.web.server.crud.EntityCrudKitRegistry;
import edu.stanford.bmir.protege.web.server.crud.PrefixedNameExpander;
import edu.stanford.bmir.protege.web.server.crud.ProjectEntityCrudKitHandlerCache;
import edu.stanford.bmir.protege.web.server.crud.persistence.ProjectEntityCrudKitSettingsRepository;
import edu.stanford.bmir.protege.web.server.index.AxiomsByEntityReferenceIndex;
import edu.stanford.bmir.protege.web.server.index.ProjectOntologiesIndex;
import edu.stanford.bmir.protege.web.server.index.RootIndex;
import edu.stanford.bmir.protege.web.server.index.impl.AxiomsByEntityReferenceIndexImpl;
import edu.stanford.bmir.protege.web.server.index.impl.ProjectOntologiesIndexImpl;
import edu.stanford.bmir.protege.web.server.inject.OverridableFileFactory;
import edu.stanford.bmir.protege.web.server.jackson.ObjectMapperProvider;
import edu.stanford.bmir.protege.web.server.lang.ActiveLanguagesManager;
import edu.stanford.bmir.protege.web.server.lang.ActiveLanguagesManagerImpl;
import edu.stanford.bmir.protege.web.server.lang.LanguageManager;
import edu.stanford.bmir.protege.web.server.owlapi.OWLEntityCreator;
import edu.stanford.bmir.protege.web.server.project.BuiltInPrefixDeclarations;
import edu.stanford.bmir.protege.web.server.project.BuiltInPrefixDeclarationsLoader;
import edu.stanford.bmir.protege.web.server.project.DefaultOntologyIdManager;
import edu.stanford.bmir.protege.web.server.project.DefaultOntologyIdManagerImpl;
import edu.stanford.bmir.protege.web.server.project.PrefixDeclarationsStore;
import edu.stanford.bmir.protege.web.server.project.ProjectDetailsRepository;
import edu.stanford.bmir.protege.web.server.revision.RevisionManager;
import edu.stanford.bmir.protege.web.server.revision.RevisionManagerImpl;
import edu.stanford.bmir.protege.web.server.shortform.DictionaryManager;
import edu.stanford.bmir.protege.web.server.shortform.LuceneEntityDocumentTranslator;
import edu.stanford.bmir.protege.web.server.shortform.LuceneEntityDocumentTranslatorImpl;
import edu.stanford.bmir.protege.web.server.shortform.LuceneIndex;
import edu.stanford.bmir.protege.web.server.shortform.LuceneIndexImpl;
import edu.stanford.bmir.protege.web.server.shortform.MultiLingualDictionary;
import edu.stanford.bmir.protege.web.server.shortform.MultiLingualDictionaryLuceneImpl;
import edu.stanford.bmir.protege.web.server.shortform.MultiLingualShortFormDictionary;
import edu.stanford.bmir.protege.web.server.shortform.MultiLingualShortFormDictionaryLuceneImpl;
import edu.stanford.bmir.protege.web.shared.crud.EntityCrudKitSuffixSettings;
import edu.stanford.bmir.protege.web.shared.crud.EntityShortForm;
import edu.stanford.bmir.protege.web.shared.entity.FreshEntityIri;
import edu.stanford.bmir.protege.web.shared.permissions.PermissionDeniedException;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

class ChangeMgr implements HasApplyChanges {
  @Nonnull
  private final ProjectId projectId;
  
  @Nonnull
  private final OWLDataFactory dataFactory;

  
  @Nonnull
  private Provider<ProjectId> projectIdProvider;
  
  @Nonnull
  private final Path baseDataDirectory;
  
  @Nonnull
  protected final Lock changeProcesssingLock = new ReentrantLock();
  
  @Nonnull
  protected final PrefixDeclarationsStore prefixDeclarationsStore;
  
  @Nonnull
  private final BuiltInPrefixDeclarations builtInPrefixDeclarations;

  
  @Nonnull
  protected RootIndex rootIndex;
  
  @Nonnull
  protected final ProjectOntologiesIndex projectOntologiesIndex;
  
  @Nonnull
  protected MongoDatabase database;
  
  @Nonnull
  private final DefaultOntologyIdManager defaultOntologyIdManager;
  
  @Nonnull
  private Provider<ObjectMapper> objectMapperProvider;
  
  @Nonnull
  private Provider<MongoDatabase> mongoDatabaseProvider;
  
  @Nonnull
  private Provider<ProjectDetailsRepository> projectDetailsRepositoryProvider;
  
  @Nonnull
  private final EntityCrudContextFactory entityCrudContextFactory;
  
  @Nonnull
  private final ProjectEntityCrudKitHandlerCache entityCrudKitHandlerCache;
  
  @Nonnull
  private final DictionaryManager dictionaryManager;
  
  @Nonnull
  private final RevisionManager revisionManager;


  
  public ChangeMgr(
    @Nonnull ProjectId projectId,
    //@Nonnull MongoDatabase database,
    @Nonnull Path baseDataDirectory,
    @Nonnull String mongodbUri,
    @Nonnull RevisionManager revisionManager
  ) {
    this.projectId = projectId;
    this.projectIdProvider = new WPImportUtil.ProjectIdProvider(projectId);
    this.mongoDatabaseProvider = new WPImportUtil.DatabaseProvider(mongodbUri);
    this.baseDataDirectory = baseDataDirectory;
    this.revisionManager = revisionManager;
    this.projectOntologiesIndex = new ProjectOntologiesIndexImpl();
    this.defaultOntologyIdManager = new DefaultOntologyIdManagerImpl(this.projectOntologiesIndex);
    
    this.objectMapperProvider = new ObjectMapperProvider();
    this.prefixDeclarationsStore = new PrefixDeclarationsStore(objectMapperProvider.get(), database);
    this.builtInPrefixDeclarations = WPImportUtil.getBuiltInPrefixDeclarations(baseDataDirectory);
    this.projectDetailsRepositoryProvider = new WPImportUtil.ProjectDetailsRepositoryProvider(this.mongoDatabaseProvider, this.objectMapperProvider);
    this.entityCrudContextFactory = new EntityCrudContextFactory(this.projectIdProvider, this.projectDetailsRepositoryProvider);
    
    WPImportUtil.OWLDataFactoryProvider owlDataFactoryProvider = new WPImportUtil.OWLDataFactoryProvider();
    this.dataFactory = owlDataFactoryProvider.get();
    
  
    AxiomsByEntityReferenceIndex aberi = new AxiomsByEntityReferenceIndexImpl(this.dataFactory);
    ProjectOntologiesIndex poi = new ProjectOntologiesIndexImpl();
    ((ProjectOntologiesIndexImpl)poi).init(revisionManager);
    
    ActiveLanguagesManager alm = new ActiveLanguagesManagerImpl(this.projectId, aberi, poi);
   

    ProjectEntityCrudKitSettingsRepository pecksr = new ProjectEntityCrudKitSettingsRepository(mongoDatabaseProvider.get(), objectMapperProvider.get());
    
    

    EntityCrudKitPluginManager eckpm = new EntityCrudKitPluginManager(null);
    EntityCrudKitRegistry eckr = new EntityCrudKitRegistry(eckpm);
    

    this.entityCrudKitHandlerCache = new ProjectEntityCrudKitHandlerCache(pecksr, projectId, eckr);
   
    LanguageManager lm = new LanguageManager(projectId, alm, this.projectDetailsRepositoryProvider.get());
    
    LuceneEntityDocumentTranslator ledt = new LuceneEntityDocumentTranslatorImpl(null, null, null, null, null, null, null, dataFactory);
    
    LuceneIndex li = new LuceneIndexImpl(ledt, null, null, null, null);
    
    MultiLingualShortFormDictionary mlsfd = new MultiLingualShortFormDictionaryLuceneImpl(li);
    MultiLingualDictionary dictionary = new MultiLingualDictionaryLuceneImpl(mlsfd, null, null);
    
    this.dictionaryManager = new DictionaryManager(lm, null, null, null);
  }
  
  Set<EntityCrudKitPlugin<?,?,?>> createPlugins(@Nonnull ProjectOntologiesIndex projectOntologiesIndex) {
    Set<EntityCrudKitPlugin<?,?,?>> plugins = new HashSet<>();
    return plugins;
  }

  @Override
  public <R> ChangeApplicationResult<R> applyChanges(@Nonnull UserId userId,
    @Nonnull ChangeListGenerator<R> changeListGenerator)
    throws PermissionDeniedException {
    checkNotNull(userId);
    checkNotNull(changeListGenerator);
    
    // Final check of whether the user can actually edit the project
    // throwEditPermissionDeniedIfNecessary(userId);

    final ChangeApplicationResult<R> changeApplicationResult;
    var crudContext = getEntityCrudContext(userId);

    // The following must take into consideration fresh entity IRIs.  Entity IRIs are minted on the server, so
    // ontology changes may contain fresh entity IRIs as place holders. We need to make sure these get replaced
    // with true entity IRIs
    try {
        // Compute the changes that need to take place.  We don't allow any other writes here because the
        // generation of the changes may depend upon the state of the project
        changeProcesssingLock.lock();

        var changeList = changeListGenerator.generateChanges(new ChangeGenerationContext(userId));

        // We have our changes
        var changes = changeList.getChanges();

        // We coin fresh IRIs for entities that have IRIs that follow the temp IRI pattern
        // See DataFactory#isFreshEntity
        var tempIri2MintedIri = new HashMap<IRI, IRI>();

        var changeSession = getEntityCrudKitHandler().createChangeSetSession();
        // Changes that refer to entities that have temp IRIs
        var changesToBeRenamed = new HashSet<OntologyChange>();
        // Changes required to create fresh entities
        var changesToCreateFreshEntities = new ArrayList<OntologyChange>();
        for(var change : changes) {
          change.getSignature()
                .forEach(entityInSignature -> {
                    if(isFreshEntity(entityInSignature)) {
                        //throwCreatePermissionDeniedIfNecessary(entityInSignature, userId);
                        changesToBeRenamed.add(change);
                        var tempIri = entityInSignature.getIRI();
                        if(!tempIri2MintedIri.containsKey(tempIri)) {
                            var freshEntityIri = FreshEntityIri.parse(tempIri.toString());
                            var shortName = freshEntityIri.getSuppliedName();
                            var langTag = Optional.<String>empty();
                            if(!shortName.isEmpty()) {
                                langTag = Optional.of(freshEntityIri.getLangTag());
                            }
                            var entityType = entityInSignature.getEntityType();
                            var discriminator = freshEntityIri.getDiscriminator();
                            var parents = freshEntityIri.getParentEntities(dataFactory, entityType);
                            var creator = getEntityCreator(changeSession,
                                                           crudContext,
                                                           shortName,
                                                           discriminator,
                                                           langTag,
                                                           parents,
                                                           entityType);
                            changesToCreateFreshEntities.addAll(creator.getChanges());
                            var mintedIri = creator.getEntity()
                                                   .getIRI();
                            tempIri2MintedIri.put(tempIri, mintedIri);
                        }
                    }
                });
          // if(isChangeForAnnotationAssertionWithFreshIris(change)) {
          //    changesToBeRenamed.add(change);
          }
    } catch (Exception e) {
      
    }
    
    return null;
  }
  
  @SuppressWarnings("unchecked")
  private <S extends EntityCrudKitSuffixSettings, C extends ChangeSetEntityCrudSession> EntityCrudKitHandler<S, C> getEntityCrudKitHandler() {
      return (EntityCrudKitHandler<S, C>) entityCrudKitHandlerCache.getHandler();
  }
  
  private static boolean isFreshEntity(OWLEntity entity) {
    return FreshEntityIri.isFreshEntityIri(entity.getIRI());
  }
  
  private EntityCrudContext getEntityCrudContext(UserId userId) {
    var prefixNameExpanderBuilder = PrefixedNameExpander.builder();
    prefixDeclarationsStore.find(projectId)
                           .getPrefixes()
                           .forEach(prefixNameExpanderBuilder::withPrefixNamePrefix);
    builtInPrefixDeclarations.getPrefixDeclarations()
                             .forEach(decl -> prefixNameExpanderBuilder.withPrefixNamePrefix(decl.getPrefixName(),
                                                                                             decl.getPrefix()));
    var prefixNameExpander = prefixNameExpanderBuilder.build();
    var defaultOntologyId = defaultOntologyIdManager.getDefaultOntologyId();
    return entityCrudContextFactory.create(userId,
                                           prefixNameExpander,
                                           defaultOntologyId);
  }
  
  private <E extends OWLEntity> OWLEntityCreator<E> getEntityCreator(
    ChangeSetEntityCrudSession session, EntityCrudContext context, String shortName,
    String discriminator, Optional<String> langTag, ImmutableList<OWLEntity> parents,
    EntityType<E> entityType) {
    if (discriminator.isEmpty() && !shortName.isEmpty()) {
      Optional<E> entity = getEntityOfTypeIfPresent(entityType, shortName);
      if (entity.isPresent()) {
        return new OWLEntityCreator<>(entity.get(), Collections.emptyList());
      }
    }
    OntologyChangeList.Builder<E> builder = OntologyChangeList.builder();
    EntityCrudKitHandler<EntityCrudKitSuffixSettings, ChangeSetEntityCrudSession> handler = getEntityCrudKitHandler();
    handler.createChangeSetSession();
    E ent = handler.create(session, entityType, EntityShortForm.get(shortName), langTag, parents,
      context, builder);
    // Generate changes to apply annotations
    // generatedAnnotationsGenerator.generateAnnotations(ent,
    //     parents,
    //     getEntityCrudKitHandler().getSettings(),
    //     builder)
    // ;
    return new OWLEntityCreator<>(ent, builder.build(ent).getChanges());

  }
  
  @SuppressWarnings("unchecked")
  private <E extends OWLEntity> Optional<E> getEntityOfTypeIfPresent(EntityType<E> entityType,
                                                                     String shortName) {
      return dictionaryManager
              .getEntities(shortName)
              .filter(entity -> entity.getEntityType()
                                      .equals(entityType))
              .map(entity -> (E) entity)
              .findFirst();

  }
  
}