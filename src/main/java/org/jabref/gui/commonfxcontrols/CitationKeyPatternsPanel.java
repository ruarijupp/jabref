package org.jabref.gui.commonfxcontrols;

import java.util.Collection;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.citationkeypattern.AbstractCitationKeyPatterns;
import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.EntryType;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class CitationKeyPatternsPanel extends TableView<CitationKeyPatternsPanelItemModel> {

    @FXML public TableColumn<CitationKeyPatternsPanelItemModel, EntryType> entryTypeColumn;
    @FXML public TableColumn<CitationKeyPatternsPanelItemModel, String> patternColumn;
    @FXML public TableColumn<CitationKeyPatternsPanelItemModel, EntryType> actionsColumn;

    @Inject private CliPreferences preferences;

    private CitationKeyPatternsPanelViewModel viewModel;

    private long lastKeyPressTime;
    private String tableSearchTerm;
    private final ObservableList<String> patterns;

    public CitationKeyPatternsPanel() {
        super();
        this.patterns = FXCollections.observableArrayList(
                CitationKeyPattern.getAllPatterns().stream()
                                  .map(CitationKeyPattern::stringRepresentation)
                                  .toList()
        );

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        viewModel = new CitationKeyPatternsPanelViewModel(preferences.getCitationKeyPatternPreferences());

        this.setEditable(true);

        entryTypeColumn.setSortable(true);
        entryTypeColumn.setReorderable(false);
        entryTypeColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<CitationKeyPatternsPanelItemModel, EntryType>()
                .withText(EntryType::getDisplayName)
                .install(entryTypeColumn);
        this.setOnSort(event ->
                viewModel.patternListProperty().sort(CitationKeyPatternsPanelViewModel.defaultOnTopComparator));

        patternColumn.setSortable(true);
        patternColumn.setReorderable(false);
        patternColumn.setCellValueFactory(cellData -> cellData.getValue().pattern());
        patternColumn.setCellFactory(_ -> new CitationKeyPatternSuggestionCell(patterns));
        patternColumn.setEditable(true);
        patternColumn.setOnEditCommit(
                (TableColumn.CellEditEvent<CitationKeyPatternsPanelItemModel, String> event) ->
                        event.getRowValue().setPattern(event.getNewValue()));

        actionsColumn.setSortable(false);
        actionsColumn.setReorderable(false);
        actionsColumn.setCellValueFactory(cellData -> cellData.getValue().entryType());
        new ValueTableCellFactory<CitationKeyPatternsPanelItemModel, EntryType>()
                .withGraphic(entryType -> IconTheme.JabRefIcons.REFRESH.getGraphicNode())
                .withTooltip(entryType ->
                        Localization.lang("Reset %s to default value").formatted(entryType.getDisplayName()))
                .withOnMouseClickedEvent(item -> evt ->
                        viewModel.setItemToDefaultPattern(this.getFocusModel().getFocusedItem()))
                .install(actionsColumn);

        this.setRowFactory(item -> new HighlightTableRow());
        this.setOnKeyTyped(this::jumpToSearchKey);
        this.itemsProperty().bindBidirectional(viewModel.patternListProperty());
    }

    public void setValues(Collection<BibEntryType> entryTypeList, AbstractCitationKeyPatterns keyPattern) {
        viewModel.setValues(entryTypeList, keyPattern);
    }

    public void resetAll() {
        viewModel.resetAll();
    }

    public ListProperty<CitationKeyPatternsPanelItemModel> patternListProperty() {
        return viewModel.patternListProperty();
    }

    public ObjectProperty<CitationKeyPatternsPanelItemModel> defaultKeyPatternProperty() {
        return viewModel.defaultKeyPatternProperty();
    }

    private void jumpToSearchKey(KeyEvent keypressed) {
        if (keypressed.getCharacter() == null) {
            return;
        }

        if (System.currentTimeMillis() - lastKeyPressTime < 1000) {
            tableSearchTerm += keypressed.getCharacter().toLowerCase();
        } else {
            tableSearchTerm = keypressed.getCharacter().toLowerCase();
        }

        lastKeyPressTime = System.currentTimeMillis();

        this.getItems().stream().filter(item -> item.getEntryType().getName().toLowerCase().startsWith(tableSearchTerm))
            .findFirst().ifPresent(this::scrollTo);
    }

    private static class HighlightTableRow extends TableRow<CitationKeyPatternsPanelItemModel> {
        @Override
        public void updateItem(CitationKeyPatternsPanelItemModel item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.getEntryType() == null) {
                setStyle("");
            } else if (isSelected()) {
                setStyle("-fx-background-color: -fx-selection-bar");
            } else if (CitationKeyPatternsPanelViewModel.ENTRY_TYPE_DEFAULT_NAME.equals(item.getEntryType().getName())) {
                setStyle("-fx-background-color: -fx-default-button");
            } else {
                setStyle("");
            }
        }
    }
}
