package edu.stanford.bmir.protege.web.client.inject;

import com.google.common.collect.ImmutableList;
import com.google.gwt.storage.client.Storage;
import dagger.Module;
import dagger.Provides;
import edu.stanford.bmir.protege.web.client.bulkop.*;
import edu.stanford.bmir.protege.web.client.change.ChangeListView;
import edu.stanford.bmir.protege.web.client.change.ChangeListViewImpl;
import edu.stanford.bmir.protege.web.client.crud.EntityCrudKitSettingsEditor;
import edu.stanford.bmir.protege.web.client.crud.EntityCrudKitSettingsEditorImpl;
import edu.stanford.bmir.protege.web.client.crud.obo.UserIdRangeEditor;
import edu.stanford.bmir.protege.web.client.crud.obo.UserIdRangeEditorImpl;
import edu.stanford.bmir.protege.web.client.editor.EditorManagerSelector;
import edu.stanford.bmir.protege.web.client.editor.EntityManagerSelectorImpl;
import edu.stanford.bmir.protege.web.client.entity.CreateEntitiesDialogViewImpl;
import edu.stanford.bmir.protege.web.client.entity.CreateEntityDialogView;
import edu.stanford.bmir.protege.web.client.entity.MergeEntitiesView;
import edu.stanford.bmir.protege.web.client.entity.MergeEntitiesViewImpl;
import edu.stanford.bmir.protege.web.client.form.*;
import edu.stanford.bmir.protege.web.client.frame.ManchesterSyntaxFrameEditor;
import edu.stanford.bmir.protege.web.client.frame.ManchesterSyntaxFrameEditorImpl;
import edu.stanford.bmir.protege.web.client.hierarchy.*;
import edu.stanford.bmir.protege.web.client.individualslist.IndividualsListView;
import edu.stanford.bmir.protege.web.client.individualslist.IndividualsListViewImpl;
import edu.stanford.bmir.protege.web.client.lang.*;
import edu.stanford.bmir.protege.web.client.list.EntityNodeListPopupView;
import edu.stanford.bmir.protege.web.client.list.EntityNodeListPopupViewImpl;
import edu.stanford.bmir.protege.web.client.match.*;
import edu.stanford.bmir.protege.web.client.ontology.annotations.AnnotationsView;
import edu.stanford.bmir.protege.web.client.ontology.annotations.AnnotationsViewImpl;
import edu.stanford.bmir.protege.web.client.permissions.LoggedInUserProjectPermissionChecker;
import edu.stanford.bmir.protege.web.client.permissions.LoggedInUserProjectPermissionCheckerImpl;
import edu.stanford.bmir.protege.web.client.perspective.PerspectiveLinkManager;
import edu.stanford.bmir.protege.web.client.perspective.PerspectiveLinkManagerImpl;
import edu.stanford.bmir.protege.web.client.portlet.PortletFactory;
import edu.stanford.bmir.protege.web.client.portlet.PortletFactoryGenerated;
import edu.stanford.bmir.protege.web.client.portlet.PortletModulesGenerated;
import edu.stanford.bmir.protege.web.client.project.*;
import edu.stanford.bmir.protege.web.client.projectsettings.*;
import edu.stanford.bmir.protege.web.client.renderer.AnnotationPropertyIriRenderer;
import edu.stanford.bmir.protege.web.client.renderer.AnnotationPropertyIriRendererImpl;
import edu.stanford.bmir.protege.web.client.renderer.ClassIriRenderer;
import edu.stanford.bmir.protege.web.client.renderer.ClassIriRendererImpl;
import edu.stanford.bmir.protege.web.client.sharing.SharingSettingsView;
import edu.stanford.bmir.protege.web.client.sharing.SharingSettingsViewImpl;
import edu.stanford.bmir.protege.web.client.tag.*;
import edu.stanford.bmir.protege.web.client.viz.LargeGraphMessageView;
import edu.stanford.bmir.protege.web.client.viz.LargeGraphMessageViewImpl;
import edu.stanford.bmir.protege.web.client.viz.VizView;
import edu.stanford.bmir.protege.web.client.viz.VizViewImpl;
import edu.stanford.bmir.protege.web.client.watches.WatchView;
import edu.stanford.bmir.protege.web.client.watches.WatchViewImpl;
import edu.stanford.bmir.protege.web.shared.entity.EntityNode;
import edu.stanford.bmir.protege.web.shared.form.field.FormElementDescriptor;
import edu.stanford.bmir.protege.web.shared.form.field.GridColumnDescriptor;
import edu.stanford.bmir.protege.web.shared.inject.ProjectSingleton;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.protege.gwt.graphtree.client.MultiSelectionModel;
import edu.stanford.protege.gwt.graphtree.client.TreeWidget;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import javax.inject.Provider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 4 Oct 2016
 */
@Module(includes = PortletModulesGenerated.class)
public class ClientProjectModule {

    private final ProjectId projectId;

    public ClientProjectModule(@Nonnull ProjectId projectId) {
        this.projectId = checkNotNull(projectId);
    }

    @Provides
    ProjectId provideProjectId() {
        return projectId;
    }

    @Provides
    AnnotationsView provideAnnotationsView(AnnotationsViewImpl view) {
        return view;
    }

    @Provides
    ManchesterSyntaxFrameEditor provideManchesterSyntaxFrameEditor(ManchesterSyntaxFrameEditorImpl editor) {
        return editor;
    }

    @Provides
    ShowProjectDetailsHandler provideShowProjectDetailsHandler(ShowProjectDetailsHandlerImpl handler) {
        return handler;
    }

    @Provides
    UploadAndMergeHandler provideUploadAndMergeHandler(UploadAndMergeHandlerImpl handler) {
        return handler;
    }

    @Provides
    LoggedInUserProjectPermissionChecker provideLoggedInUserProjectPermissionChecker(LoggedInUserProjectPermissionCheckerImpl checker) {
        return checker;
    }

    @Provides
    EditorManagerSelector provideEditorManagerSelector(EntityManagerSelectorImpl selector) {
        return selector;
    }

    @Provides
    ChangeListView provideChangeListView(ChangeListViewImpl view) {
        return view;
    }

    @Provides
    SharingSettingsView provideSharingSettingsView(SharingSettingsViewImpl view) {
        return view;
    }

    @Provides
    IndividualsListView provideIndividualsListView(IndividualsListViewImpl view) {
        return view;
    }

    @Provides
    WatchView provideWatchTypeSelectorView(WatchViewImpl view) {
        return view;
    }

    @Provides
    FormView provideFormView(FormViewImpl view) {
        return view;
    }

    @Provides
    EntityCrudKitSettingsEditor provideEntityCrudKitSettingsEditor(EntityCrudKitSettingsEditorImpl editor) {
        return editor;
    }

    @Provides
    UserIdRangeEditor provideUserIdRangeEditor(UserIdRangeEditorImpl editor) {
        return editor;
    }


    @Provides
    PerspectiveLinkManager providePerspectiveLinkManager(PerspectiveLinkManagerImpl linkManager) {
        return linkManager;
    }


    @Provides
    @ProjectSingleton
    PortletFactory providePortletFactory(PortletFactoryGenerated portletFactoryGenerated) {
        return portletFactoryGenerated;
    }

    @Provides
    TreeWidget<EntityNode, OWLEntity> providesEntityHierarchyTree() {
        MultiSelectionModel selectionModel = new MultiSelectionModel();
        return new TreeWidget<>(selectionModel);
    }

    @Provides
    PropertyHierarchyPortletView providePropertyHierarchyPortletView(PropertyHierarchyPortletViewImpl impl) {
        return impl;
    }

    @Provides
    CreateEntityDialogView providesCreateEntityDialogView(CreateEntitiesDialogViewImpl impl) {
        return impl;
    }

    @Provides
    BlankCriteriaView provideEntityIsDeprecatedCriteriaView(BlankCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    AnnotationCriteriaView provideEntityAnnotationCriteriaView(AnnotationCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    SelectableCriteriaTypeView provideSelectableCriteriaTypeView(SelectableCriteriaTypeViewImpl impl) {
        return impl;
    }

    @Provides
    SimpleStringCriteriaView provideSimpleStringCriteriaView(SimpleStringCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    NumericValueCriteriaView provideNumericValueCriteriaView(NumericValueCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    LangTagMatchesCriteriaView provideLangTagMatchesCriteriaView(LangTagMatchesCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    DateView provideDateTimeView(DateViewImpl impl) {
        return impl;
    }

    @Provides
    QueryPortletView provideMatchPortletView(QueryPortletViewImpl impl) {
        return impl;
    }

    @Provides
    CriteriaListView provideCriteriaListView(CriteriaListViewImpl impl) {
        return impl;
    }

    @Provides
    CriteriaListCriteriaViewContainer provideCriteriaListCriteriaViewContainer(CriteriaListViewViewContainerImpl impl) {
        return impl;
    }

    @Provides
    AnnotationPropertyCriteriaView provideAnnotationPropertyCriteriaView(AnnotationPropertyCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    EntityTypeCriteriaView provideEntityTypeCriteriaView(EntityTypeCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    IriEqualsView provideIriEqualsView(IriEqualsViewImpl impl) {
        return impl;
    }

    @Provides
    AnnotationPropertyPairView provideAnnotationPropertyPairView(AnnotationPropertyPairViewImpl impl) {
        return impl;
    }

    @Provides
    ClassSelectorView provideClassView(ClassSelectorViewImpl impl) {
        return impl;
    }

    @Provides
    TagCriteriaView provideTagCriteriaView(TagCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    TagCriteriaListView provideTagCriteriaListView(TagCriteriaListViewImpl impl) {
        return impl;
    }

    @Provides
    TagCriteriaViewContainer provideTagCriteriaViewContainer(TagCriteriaViewContainerImpl impl) {
        return impl;
    }

    @Provides
    AnnotationPropertyIriRenderer provideAnnotationPropertyIriRenderer(AnnotationPropertyIriRendererImpl impl) {
        return impl;
    }

    @Provides
    ClassIriRenderer provideClassIriRenderer(ClassIriRendererImpl impl) {
        return impl;
    }

    @Provides
    GeneralSettingsView provideGeneralSettingsView(GeneralSettingsViewImpl impl) {
        return impl;
    }

    @Provides
    SlackWebhookSettingsView provideSlackWebhookSettingsView(SlackWebhookSettingsViewImpl impl) {
        return impl;
    }

    @Provides
    WebhookSettingsView provideWebhookSettingsView(WebhookSettingsViewImpl impl) {
        return impl;
    }

    @Provides
    DefaultDisplayNameSettingsView provideDisplayLanguagesView(DefaultDisplayNameSettingsViewImpl impl) {
        return impl;
    }

    @Provides
    DefaultDictionaryLanguageView provideEntityDefaultLanguagesView(DefaultDictionaryLanguageViewImpl impl) {
        return impl;
    }

    @Provides
    DictionaryLanguageDataView provideDictionaryLanguageDataView(DictionaryLanguageDataViewImpl impl) {
        return impl;
    }

    @Provides
    DefaultLanguageTagView provideDefaultLanguageTagView(DefaultLanguageTagViewImpl impl) {
        return impl;
    }

    @Provides
    DisplayNameSettingsTopBarView providePreferredLanguageView(DisplayNameSettingsTopBarViewImpl impl) {
        return impl;
    }

    @Provides
    DisplayNameSettingsView provideDisplayLanguageEditorView(DisplayNameSettingsViewImpl impl) {
        return impl;
    }

    @Provides
    Storage provideLocalStorage() {
        return Storage.getLocalStorageIfSupported();
    }

    @Provides
    LanguageUsageView provideLanguageUsageView(LanguageUsageViewImpl impl) {
        return impl;
    }

    @Provides
    HierarchyFieldView provideHierarchyFieldView(HierarchyFieldViewImpl impl) {
        return impl;
    }

    @Provides
    EntityNodeListPopupView provideEntityNodeListPopupView(EntityNodeListPopupViewImpl impl) {
        return impl;
    }

    @Provides
    HierarchyPopupView provideHierarchyPopupView(HierarchyPopupViewImpl impl) {
        return impl;
    }

    @Provides
    BulkEditOperationViewContainer provideBulkEditOperationViewContainer(BulkEditOperationViewContainerImpl impl) {
        return impl;
    }

    @Provides
    SetAnnotationValueView provideSetAnnotationValueView(SetAnnotationValueViewImpl impl) {
        return impl;
    }

    @Provides
    EditAnnotationsView provideReplaceAnnotationValuesView(EditAnnotationsViewImpl impl) {
        return impl;
    }

    @Provides
    MoveToParentView provideMoveToParentView(MoveToParentViewImpl impl) {
        return impl;
    }

    @Provides
    MergeEntitiesView provideMergeEntitiesView(MergeEntitiesViewImpl impl) {
        return impl;
    }

    @Provides
    AnnotationSimpleMatchingCriteriaView provideAnnotationSimpleMatchingCriteriaView(AnnotationSimpleMatchingCriteriaViewImpl impl) {
        return impl;
    }

    @Provides
    CommitMessageInputView provideCommitMessageInputView(CommitMessageInputViewImpl impl) {
        return impl;
    }

    @Provides
    VizView provideVizView(VizViewImpl view) {
        return view;
    }

    @Provides
    LargeGraphMessageView provideLargeGraphMessageView(LargeGraphMessageViewImpl impl) {
        return impl;
    }

    @Provides
    FormsManagerView provideFormsManagerView(FormsManagerViewImpl impl) {
        return impl;
    }

    @Provides
    FormElementDescriptorView provideFormElementDescriptorEditorView(FormElementDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    TextControlDescriptorView provideTextFieldDescriptorEditorView(TextControlDescriptorViewImpl view) {
        return view;
    }

    @Provides
    FormDescriptorView provideFormDescriptorView(FormDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    ObjectListView provideFormElementDescriptorListView(@Nonnull ObjectListViewImpl impl) {
        return impl;
    }

    @Provides
    ObjectListViewHolder provideFormElementDescriptorViewHolder(@Nonnull ObjectListViewHolderImpl impl) {
        return impl;
    }


    @Provides
    FormElementView provideFormElementView(FormElementViewImpl impl) {
        return impl;
    }

    @Provides
    FormViewRow provideFormViewRow(FormViewRowImpl impl) {
        return impl;
    }

    @Provides
    NoFormView provideNoFormView(NoFormViewImpl impl) {
        return impl;
    }

    @Provides
    NumberControlRangeView provideNumberFieldRangeView(@Nonnull NumberControlRangeViewImpl impl) {
        return impl;
    }

    @Provides
    NumberControlDescriptorView provideNumberFieldDescriptorView(@Nonnull NumberControlDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    ChoiceDescriptorView provideChoiceDescriptorView(@Nonnull ChoiceDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    ChoiceControlDescriptorView provideChoiceFieldDescriptorView(ChoiceControlDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    LanguageMapEntryView provideLanguageMapEntryView(LanguageMapEntryViewImpl impl) {
        return impl;
    }

    @Provides
    NoFieldDescriptorView provideNoFieldDescriptorView(NoFieldDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    ImageDescriptorView provideImageDescriptorView(ImageDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    EntityNameFieldDescriptorView provideEntityNameFieldDescriptorView(@Nonnull EntityNameFieldDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    SubFormControlDescriptorView provideSubFormFieldDescriptorView(@Nonnull SubFormControlDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    NoFormDescriptorSelectedView provideNoFormDescriptorSelectedView(NoFormDescriptorSelectedViewImpl impl) {
        return impl;
    }

    @Provides
    GridView provideGridView(GridViewImpl impl) {
        return impl;
    }

    @Provides
    GridRowView provideGridRowView(GridRowViewImpl impl) {
        return impl;
    }

    @Provides
    GridCellView provideGridCellView(GridCellViewImpl impl) {
        return impl;
    }

    @Provides
    GridControlDescriptorView provideGridFieldDescriptorView(GridControlDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    ObjectListPresenter<FormElementDescriptor> provideFormElementDescriptorListPresenter(@Nonnull ObjectListView view,
                                                                                         @Nonnull Provider<ObjectPresenter<FormElementDescriptor>> objectPresenterProvider,
                                                                                         @Nonnull Provider<ObjectListViewHolder> objectViewHolderProvider) {
        return new ObjectListPresenter<>(view, objectPresenterProvider, objectViewHolderProvider, FormElementDescriptor::getDefault);
    }

    @Provides
    ObjectPresenter<FormElementDescriptor> providesFormElementDescriptorPresenter(FormElementDescriptorPresenter presenter) {
        return presenter;
    }

    @Provides
    ImmutableList<FormControlDescriptorPresenterFactory> provideFormFieldDescriptorPresenterFactories(
            TextControlDescriptorPresenterFactory textFieldDescriptorEditorPresenterFactory,
            NumberControlDescriptorPresenterFactory numberFieldDescriptorPresenterFactory,
            ChoiceControlDescriptorPresenterFactory choiceFieldDescriptorPresenterFactory,
            ImageDescriptorPresenterFactory imageDescriptorPresenterFactory,
            EntityNameControlDescriptorPresenterFactory entityNameFieldDescriptorPresenterFactory,
            SubFormControlDescriptorPresenterFactory subFormControlDescriptorPresenterFactory,
            GridControlDescriptorPresenterFactory gridControlDescriptorPresenterFactory) {
        return ImmutableList.of(textFieldDescriptorEditorPresenterFactory,
                                numberFieldDescriptorPresenterFactory,
                                choiceFieldDescriptorPresenterFactory,
                                imageDescriptorPresenterFactory,
                                entityNameFieldDescriptorPresenterFactory,
                                subFormControlDescriptorPresenterFactory,
                                gridControlDescriptorPresenterFactory);
    }

    @Provides
    FormControlDescriptorChooserView providesFormFieldDescriptorChooserView(FormControlDescriptorChooserViewImpl impl) {
        return impl;
    }

    @Provides
    ObjectListPresenter<GridColumnDescriptor> provideGridColumnDescriptorListPresenter(ObjectListView view,
                                                                                       Provider<ObjectPresenter<GridColumnDescriptor>> gridColumnDescriptorPresenterProvider,
                                                                                       Provider<ObjectListViewHolder> objectViewHolderProvider) {
        return new ObjectListPresenter<>(view,
                                         gridColumnDescriptorPresenterProvider,
                                         objectViewHolderProvider,
                                         GridColumnDescriptor::getDefaultColumnDescriptor);
    }

    @Provides
    ObjectPresenter<GridColumnDescriptor> provideGridColumnDescriptor(GridColumnDescriptorPresenter presenter) {
        return presenter;
    }

    @Provides
    GridColumnDescriptorView provideGridColumnDescriptorView(GridColumnDescriptorViewImpl impl) {
        return impl;
    }

    @Provides
    OwlBindingView provideOwlBindingView(OwlBindingViewImpl impl) {
        return impl;
    }

    @Provides
    GridHeaderView provideGridHeaderView(GridHeaderViewImpl impl) {
        return impl;
    }

    @Provides
    GridHeaderColumnView provideGridColumnHeaderView(GridHeaderColumnViewImpl view) {
        return view;
    }

}


