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

package com.owlplug.recipe.services;

import com.owlplug.plugin.model.Plugin;
import com.owlplug.plugin.repositories.PluginRepository;
import com.owlplug.project.model.DawProject;
import com.owlplug.project.repositories.DawProjectRepository;
import com.owlplug.recipe.events.RecipeUpdateEvent;
import com.owlplug.recipe.model.Recipe;
import com.owlplug.recipe.repositories.RecipeRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

  @Autowired
  private RecipeRepository recipeRepository;
  @Autowired
  private PluginRepository pluginRepository;
  @Autowired
  private DawProjectRepository dawProjectRepository;
  @Autowired
  private ApplicationEventPublisher publisher;

  public List<Recipe> getAllRecipes() {
    return recipeRepository.findAll();
  }

  public Optional<Recipe> getRecipeById(Long id) {
    return recipeRepository.findById(id);
  }

  public List<Recipe> getRecipesByPlugin(Plugin plugin) {
    return recipeRepository.findByPluginsId(plugin.getId());
  }

  public List<Recipe> getRecipesByProject(DawProject project) {
    return recipeRepository.findByProjectsId(project.getId());
  }

  @Transactional
  public Recipe createRecipe(String name) {
    Recipe recipe = new Recipe();
    recipe.setName(name);
    Date now = new Date();
    recipe.setCreatedAt(now);
    recipe.setUpdatedAt(now);
    Recipe savedRecipe = recipeRepository.save(recipe);
    publisher.publishEvent(new RecipeUpdateEvent());
    return savedRecipe;
  }

  @Transactional
  public Recipe saveRecipe(Recipe recipe) {
    recipe.setUpdatedAt(new Date());
    Recipe savedRecipe = recipeRepository.save(recipe);
    publisher.publishEvent(new RecipeUpdateEvent());
    return savedRecipe;
  }

  @Transactional
  public void deleteRecipe(Recipe recipe) {
    recipeRepository.delete(recipe);
    publisher.publishEvent(new RecipeUpdateEvent());
  }

  @Transactional
  public Recipe addPluginToRecipe(Recipe recipe, Plugin plugin) {
    Recipe managedRecipe = recipeRepository.findById(recipe.getId()).orElseThrow();
    Plugin managedPlugin = pluginRepository.findById(plugin.getId()).orElseThrow();
    applyEditableFields(managedRecipe, recipe);
    managedRecipe.getPlugins().add(managedPlugin);
    managedRecipe.setUpdatedAt(new Date());
    Recipe savedRecipe = recipeRepository.save(managedRecipe);
    publisher.publishEvent(new RecipeUpdateEvent());
    return savedRecipe;
  }

  @Transactional
  public Recipe removePluginFromRecipe(Recipe recipe, Plugin plugin) {
    Recipe managedRecipe = recipeRepository.findById(recipe.getId()).orElseThrow();
    applyEditableFields(managedRecipe, recipe);
    managedRecipe.getPlugins().removeIf(p -> p.getId().equals(plugin.getId()));
    managedRecipe.setUpdatedAt(new Date());
    Recipe savedRecipe = recipeRepository.save(managedRecipe);
    publisher.publishEvent(new RecipeUpdateEvent());
    return savedRecipe;
  }

  @Transactional
  public Recipe addProjectToRecipe(Recipe recipe, DawProject project) {
    Recipe managedRecipe = recipeRepository.findById(recipe.getId()).orElseThrow();
    DawProject managedProject = dawProjectRepository.findById(project.getId()).orElseThrow();
    applyEditableFields(managedRecipe, recipe);
    managedRecipe.getProjects().add(managedProject);
    managedRecipe.setUpdatedAt(new Date());
    Recipe savedRecipe = recipeRepository.save(managedRecipe);
    publisher.publishEvent(new RecipeUpdateEvent());
    return savedRecipe;
  }

  @Transactional
  public Recipe removeProjectFromRecipe(Recipe recipe, DawProject project) {
    Recipe managedRecipe = recipeRepository.findById(recipe.getId()).orElseThrow();
    applyEditableFields(managedRecipe, recipe);
    managedRecipe.getProjects().removeIf(p -> p.getId().equals(project.getId()));
    managedRecipe.setUpdatedAt(new Date());
    Recipe savedRecipe = recipeRepository.save(managedRecipe);
    publisher.publishEvent(new RecipeUpdateEvent());
    return savedRecipe;
  }

  private void applyEditableFields(Recipe target, Recipe source) {
    target.setName(source.getName());
    target.setDescription(source.getDescription());
  }
}
