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

package com.owlplug.recipe.controllers;

import com.owlplug.core.controllers.BaseController;
import com.owlplug.core.utils.FX;
import com.owlplug.plugin.model.Plugin;
import com.owlplug.plugin.services.PluginService;
import com.owlplug.project.model.DawProject;
import com.owlplug.project.services.ProjectService;
import com.owlplug.recipe.events.RecipeUpdateEvent;
import com.owlplug.recipe.model.Recipe;
import com.owlplug.recipe.services.RecipeService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

@Controller
public class RecipesController extends BaseController {

  @Autowired
  private RecipeService recipeService;
  @Autowired
  private PluginService pluginService;
  @Autowired
  private ProjectService projectService;

  @FXML
  private TextField newRecipeNameTextField;
  @FXML
  private Button createRecipeButton;
  @FXML
  private ListView<Recipe> recipeListView;
  @FXML
  private VBox recipeDetailsPane;
  @FXML
  private TextField recipeNameTextField;
  @FXML
  private TextArea recipeDescriptionTextArea;
  @FXML
  private Button saveRecipeButton;
  @FXML
  private Button deleteRecipeButton;
  @FXML
  private ListView<Plugin> linkedPluginListView;
  @FXML
  private ListView<Plugin> availablePluginListView;
  @FXML
  private TextField pluginSearchTextField;
  @FXML
  private Button addPluginButton;
  @FXML
  private Button removePluginButton;
  @FXML
  private ListView<DawProject> linkedProjectListView;
  @FXML
  private ListView<DawProject> availableProjectListView;
  @FXML
  private TextField projectSearchTextField;
  @FXML
  private Button addProjectButton;
  @FXML
  private Button removeProjectButton;

  private List<Plugin> plugins = new ArrayList<>();
  private List<DawProject> projects = new ArrayList<>();

  @FXML
  public void initialize() {
    recipeDetailsPane.setManaged(false);
    recipeDetailsPane.setVisible(false);

    recipeListView.setCellFactory(e -> new RecipeCell());
    linkedProjectListView.setCellFactory(e -> new ProjectCell());
    availableProjectListView.setCellFactory(e -> new ProjectCell());

    recipeListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      displayRecipe(newValue);
    });

    createRecipeButton.setOnAction(e -> createRecipe());
    newRecipeNameTextField.setOnAction(e -> createRecipe());
    saveRecipeButton.setOnAction(e -> saveSelectedRecipe());
    deleteRecipeButton.setOnAction(e -> deleteSelectedRecipe());
    addPluginButton.setOnAction(e -> addSelectedPlugin());
    removePluginButton.setOnAction(e -> removeSelectedPlugin());
    addProjectButton.setOnAction(e -> addSelectedProject());
    removeProjectButton.setOnAction(e -> removeSelectedProject());

    pluginSearchTextField.textProperty().addListener(e -> refreshRecipeLinks());
    projectSearchTextField.textProperty().addListener(e -> refreshRecipeLinks());

    refresh();
  }

  public void refresh() {
    Long selectedId = getSelectedRecipeId();
    plugins = StreamSupport.stream(pluginService.getAllPlugins().spliterator(), false)
        .sorted(Comparator.comparing(this::pluginName, String.CASE_INSENSITIVE_ORDER))
        .collect(Collectors.toList());
    projects = StreamSupport.stream(projectService.getAllProjects().spliterator(), false)
        .sorted(Comparator.comparing(this::projectName, String.CASE_INSENSITIVE_ORDER))
        .collect(Collectors.toList());

    List<Recipe> recipes = recipeService.getAllRecipes().stream()
        .sorted(Comparator.comparing(this::recipeName, String.CASE_INSENSITIVE_ORDER))
        .collect(Collectors.toList());
    recipeListView.setItems(FXCollections.observableList(recipes));

    if (selectedId != null) {
      recipes.stream()
          .filter(recipe -> selectedId.equals(recipe.getId()))
          .findFirst()
          .ifPresent(recipeListView.getSelectionModel()::select);
    } else if (!recipes.isEmpty()) {
      recipeListView.getSelectionModel().select(0);
    } else {
      displayRecipe(null);
    }
  }

  private void displayRecipe(Recipe recipe) {
    boolean visible = recipe != null;
    recipeDetailsPane.setManaged(visible);
    recipeDetailsPane.setVisible(visible);
    if (!visible) {
      recipeNameTextField.clear();
      recipeDescriptionTextArea.clear();
      return;
    }
    recipeNameTextField.setText(recipe.getName());
    recipeDescriptionTextArea.setText(recipe.getDescription());
    refreshRecipeLinks();
  }

  private void refreshRecipeLinks() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    if (recipe == null) {
      return;
    }

    Set<Long> linkedPluginIds = recipe.getPlugins().stream()
        .map(Plugin::getId)
        .collect(Collectors.toSet());
    List<Plugin> linkedPlugins = plugins.stream()
        .filter(plugin -> linkedPluginIds.contains(plugin.getId()))
        .collect(Collectors.toList());
    List<Plugin> availablePlugins = plugins.stream()
        .filter(plugin -> !linkedPluginIds.contains(plugin.getId()))
        .filter(plugin -> contains(pluginName(plugin), pluginSearchTextField.getText()))
        .collect(Collectors.toList());
    linkedPluginListView.setItems(FXCollections.observableList(linkedPlugins));
    availablePluginListView.setItems(FXCollections.observableList(availablePlugins));

    Set<Long> linkedProjectIds = recipe.getProjects().stream()
        .map(DawProject::getId)
        .collect(Collectors.toSet());
    List<DawProject> linkedProjects = projects.stream()
        .filter(project -> linkedProjectIds.contains(project.getId()))
        .collect(Collectors.toList());
    List<DawProject> availableProjects = projects.stream()
        .filter(project -> !linkedProjectIds.contains(project.getId()))
        .filter(project -> contains(projectName(project), projectSearchTextField.getText()))
        .collect(Collectors.toList());
    linkedProjectListView.setItems(FXCollections.observableList(linkedProjects));
    availableProjectListView.setItems(FXCollections.observableList(availableProjects));
  }

  private void createRecipe() {
    String name = newRecipeNameTextField.getText();
    if (name == null || name.isBlank()) {
      return;
    }
    Recipe recipe = recipeService.createRecipe(name.trim());
    newRecipeNameTextField.clear();
    refresh();
    recipeListView.getSelectionModel().select(recipeListView.getItems().stream()
        .filter(item -> item.getId().equals(recipe.getId()))
        .findFirst()
        .orElse(null));
  }

  private void saveSelectedRecipe() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    if (!updateRecipeFromFields(recipe)) {
      return;
    }
    recipeService.saveRecipe(recipe);
    refresh();
  }

  private void deleteSelectedRecipe() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    if (recipe == null) {
      return;
    }
    recipeService.deleteRecipe(recipe);
    refresh();
  }

  private void addSelectedPlugin() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    Plugin plugin = availablePluginListView.getSelectionModel().getSelectedItem();
    if (plugin == null || !updateRecipeFromFields(recipe)) {
      return;
    }
    recipeService.addPluginToRecipe(recipe, plugin);
    refreshSelectedRecipe(recipe.getId());
  }

  private void removeSelectedPlugin() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    Plugin plugin = linkedPluginListView.getSelectionModel().getSelectedItem();
    if (plugin == null || !updateRecipeFromFields(recipe)) {
      return;
    }
    recipeService.removePluginFromRecipe(recipe, plugin);
    refreshSelectedRecipe(recipe.getId());
  }

  private void addSelectedProject() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    DawProject project = availableProjectListView.getSelectionModel().getSelectedItem();
    if (project == null || !updateRecipeFromFields(recipe)) {
      return;
    }
    recipeService.addProjectToRecipe(recipe, project);
    refreshSelectedRecipe(recipe.getId());
  }

  private void removeSelectedProject() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    DawProject project = linkedProjectListView.getSelectionModel().getSelectedItem();
    if (project == null || !updateRecipeFromFields(recipe)) {
      return;
    }
    recipeService.removeProjectFromRecipe(recipe, project);
    refreshSelectedRecipe(recipe.getId());
  }

  private boolean updateRecipeFromFields(Recipe recipe) {
    if (recipe == null || recipeNameTextField.getText() == null || recipeNameTextField.getText().isBlank()) {
      return false;
    }
    recipe.setName(recipeNameTextField.getText().trim());
    recipe.setDescription(recipeDescriptionTextArea.getText());
    return true;
  }

  private void refreshSelectedRecipe(Long recipeId) {
    recipeService.getRecipeById(recipeId).ifPresent(recipe -> {
      recipeListView.getItems().set(recipeListView.getSelectionModel().getSelectedIndex(), recipe);
      recipeListView.getSelectionModel().select(recipe);
      displayRecipe(recipe);
    });
  }

  private Long getSelectedRecipeId() {
    Recipe recipe = recipeListView.getSelectionModel().getSelectedItem();
    return recipe != null ? recipe.getId() : null;
  }

  private boolean contains(String value, String search) {
    if (search == null || search.isBlank()) {
      return true;
    }
    return value.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
  }

  private String recipeName(Recipe recipe) {
    return recipe.getName() != null ? recipe.getName() : "";
  }

  private String pluginName(Plugin plugin) {
    return plugin.getName() != null ? plugin.getName() : "";
  }

  private String projectName(DawProject project) {
    return project.getName() != null ? project.getName() : "";
  }

  @EventListener
  private void handle(RecipeUpdateEvent event) {
    FX.run(this::refresh);
  }

  private class RecipeCell extends ListCell<Recipe> {
    @Override
    protected void updateItem(Recipe item, boolean empty) {
      super.updateItem(item, empty);
      if (item == null || empty) {
        setText(null);
        setGraphic(null);
      } else {
        Label label = new Label(item.getName() + "  (" + item.getPlugins().size() + " plugins, "
            + item.getProjects().size() + " projects)");
        setGraphic(label);
      }
    }
  }

  private class ProjectCell extends ListCell<DawProject> {
    @Override
    protected void updateItem(DawProject item, boolean empty) {
      super.updateItem(item, empty);
      if (item == null || empty) {
        setText(null);
      } else {
        setText(item.getName());
      }
    }
  }
}
