/* OwlPlug
 * Copyright (C) 2021 Arthur <dropsnorz@gmail.com>
 *
 * This file is part of OwlPlug.
 *
 * OwlPlug is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OwlPlug is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OwlPlug.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.owlplug.project.controllers;

import com.owlplug.core.controllers.BaseController;
import com.owlplug.core.controllers.MainController;
import com.owlplug.core.utils.FX;
import com.owlplug.core.utils.PlatformUtils;
import com.owlplug.core.utils.TimeUtils;
import com.owlplug.plugin.controllers.PluginsController;
import com.owlplug.plugin.model.Plugin;
import com.owlplug.plugin.model.PluginFormat;
import com.owlplug.project.model.DawPlugin;
import com.owlplug.project.model.DawProject;
import com.owlplug.project.model.LookupResult;
import com.owlplug.recipe.events.RecipeUpdateEvent;
import com.owlplug.recipe.model.Recipe;
import com.owlplug.recipe.services.RecipeService;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectInfoController extends BaseController {

  @Autowired
  private PluginsController pluginsController;
  @Autowired
  @Lazy
  private MainController mainController;
  @Autowired
  private RecipeService recipeService;

  @FXML
  private VBox projectInfoPane;
  @FXML
  private Label projectNameLabel;
  @FXML
  private ImageView projectAppImageView;
  @FXML
  private Label projectAppLabel;
  @FXML
  private Button projectOpenButton;
  @FXML
  private Label appFullNameLabel;
  @FXML
  private Label projectFormatVersionLabel;
  @FXML
  private Label projectCreatedLabel;
  @FXML
  private Label projectLastModifiedLabel;
  @FXML
  private Label projectPluginsFoundLabel;
  @FXML
  private Label projectPathLabel;
  @FXML
  private Button openDirectoryButton;
  @FXML
  private TableView<DawPlugin> pluginTable;
  @FXML
  private TableColumn<DawPlugin, PluginFormat> pluginTableFormatColumn;
  @FXML
  private TableColumn<DawPlugin, String> pluginTableNameColumn;
  @FXML
  private TableColumn<DawPlugin, String> pluginTableStatusColumn;
  @FXML
  private TableColumn<DawPlugin, Plugin> pluginTableLinkColumn;
  @FXML
  private ListView<Recipe> recipeListView;
  @FXML
  private ComboBox<Recipe> recipeComboBox;
  @FXML
  private Button addToRecipeButton;
  @FXML
  private Button removeFromRecipeButton;

  private final ObjectProperty<DawProject> projectProperty = new SimpleObjectProperty<>();


  @FXML
  public void initialize() {
    projectProperty.addListener(e -> refresh());
    openDirectoryButton.setOnAction(e -> {
      File projectFile = new File(projectPathLabel.getText());
      PlatformUtils.openFromDesktop(projectFile.getParentFile());
    });

    projectOpenButton.setOnAction(e -> {
      DawProject project = projectProperty.get();
      if (project != null) {
        PlatformUtils.openFromDesktop(project.getPath());
        // Disable to prevent opening the project several times.
        projectOpenButton.setDisable(true);
      }
    });

    // Set invisible by default if no project is selected.
    projectInfoPane.setVisible(false);

    pluginTableNameColumn.setCellValueFactory(cellData -> {
      return new SimpleStringProperty(cellData.getValue().getName());
    });
    pluginTableStatusColumn.setCellValueFactory(cellData -> {
      if (cellData.getValue().getLookup() != null
              && cellData.getValue().getLookup().getResult() != null) {
        return new SimpleStringProperty(cellData.getValue().getLookup().getResult().getValue());
      }
      return new SimpleStringProperty("Unknown");
    });
    pluginTableFormatColumn.setCellValueFactory(cellData -> {
      return new SimpleObjectProperty<>(cellData.getValue().getFormat());
    });

    pluginTableStatusColumn.setCellFactory(e -> new TableCell<>() {
      @Override
      public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        this.getStyleClass().remove("cell-unknown-link");
        this.getStyleClass().remove("cell-missing-link");
        this.getStyleClass().remove("cell-found-link");
        if (item == null || empty) {
          setText(null);
        } else {
          setText(item);
          if (item.equals(LookupResult.MISSING.getValue())) {
            this.getStyleClass().add("cell-missing-link");
          } else if (item.equals(LookupResult.FOUND.getValue())) {
            this.getStyleClass().add("cell-found-link");
          } else {
            this.getStyleClass().add("cell-unknown-link");
          }
        }
      }
    });

    pluginTableLinkColumn.setCellValueFactory(cellData -> {
      if (cellData.getValue().getLookup() != null) {
        return new SimpleObjectProperty<>(cellData.getValue().getLookup().getPlugin());
      }
      return null;
    });

    pluginTableLinkColumn.setCellFactory(e -> new TableCell<>() {
      @Override
      public void updateItem(Plugin item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          setGraphic(null);
        } else {
          Hyperlink link = new Hyperlink();
          link.setGraphic(new ImageView(getApplicationDefaults().linkIconImage));
          link.setOnAction(e -> {
            pluginsController.selectPluginById(item.getId());
            mainController.selectMainTab(MainController.PLUGINS_TAB_INDEX);
          });
          setGraphic(link);
        }
      }
    });

    pluginTableFormatColumn.setCellFactory(e -> new TableCell<>() {
      @Override
      public void updateItem(PluginFormat item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          setGraphic(null);
        } else {
          setText(item.getText());
          setGraphic(new ImageView(getApplicationDefaults().getPluginFormatIcon(item)));
        }
      }
    });

    addToRecipeButton.setOnAction(e -> {
      DawProject project = projectProperty.get();
      Recipe recipe = recipeComboBox.getSelectionModel().getSelectedItem();
      if (project != null && recipe != null) {
        recipeService.addProjectToRecipe(recipe, project);
        refreshRecipes();
      }
    });

    removeFromRecipeButton.setOnAction(e -> {
      DawProject project = projectProperty.get();
      Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
      if (project != null && recipe != null) {
        recipeService.removeProjectFromRecipe(recipe, project);
        refreshRecipes();
      }
    });

  }

  public void refresh() {
    DawProject project = projectProperty.get();
    projectInfoPane.setVisible(true);
    projectNameLabel.setText(project.getName());
    projectAppLabel.setText(project.getApplication().getName());
    projectAppImageView.setImage(this.getApplicationDefaults().getDAWApplicationIcon(project.getApplication()));
    projectOpenButton.setDisable(false);
    appFullNameLabel.setText(project.getAppFullName());
    projectCreatedLabel.setText(TimeUtils.getHumanReadableDurationFrom(project.getCreatedAt()));
    projectLastModifiedLabel.setText(TimeUtils.getHumanReadableDurationFrom(project.getLastModifiedAt()));
    projectPluginsFoundLabel.setText(String.valueOf(project.getPlugins().size()));
    projectFormatVersionLabel.setText("v" + project.getFormatVersion());
    projectPathLabel.setText(project.getPath());

    pluginTable.setItems(FXCollections.observableList(project.getPlugins().stream().toList()));
    refreshRecipes();

  }

  private void refreshRecipes() {
    DawProject project = projectProperty.get();
    if (project == null || project.getId() == null) {
      recipeListView.setItems(FXCollections.observableArrayList());
      recipeComboBox.setItems(FXCollections.observableArrayList());
      return;
    }

    List<Recipe> linkedRecipes = recipeService.getRecipesByProject(project);
    Set<Long> linkedRecipeIds = linkedRecipes.stream()
        .map(Recipe::getId)
        .collect(Collectors.toSet());
    List<Recipe> availableRecipes = recipeService.getAllRecipes().stream()
        .filter(recipe -> !linkedRecipeIds.contains(recipe.getId()))
        .collect(Collectors.toList());

    recipeListView.setItems(FXCollections.observableList(linkedRecipes));
    recipeComboBox.setItems(FXCollections.observableList(availableRecipes));
    recipeComboBox.getSelectionModel().clearSelection();
  }

  @EventListener
  private void handle(RecipeUpdateEvent event) {
    FX.run(this::refreshRecipes);
  }

  public ObjectProperty<DawProject> projectProperty() {
    return projectProperty;
  }

}
