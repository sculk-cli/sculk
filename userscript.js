// ==UserScript==
// @name         Sculk Mod Install List Creator
// @namespace    https://sculk-cli.github.io/
// @version      2024-04-27
// @description  Adds widgets to Modrinth and Curseforge to add projects to an exportable Sculk install list
// @author       Jamalam
// @match        https://*/*
// @icon         https://sculk-cli.github.io/assets/sculk-icon.png
// @grant        GM_setValue
// @grant        GM_getValue
// ==/UserScript==

const MODRINTH_HOST = "modrinth.com";
const CURSEFORGE_HOST = "www.curseforge.com";
const MODRINTH_PATHS = [
  "mod",
  "plugin", // Some plugins are mods
  "datapack",
  "shader",
  "resourcepack",
];
const CURSEFORGE_PATHS = ["mc-mods", "texture-packs", "data-packs", "shaders"];

(function () {
  "use strict";

  class List {
    constructor() {
      this.projects = [];
      this.load();
    }

    addProject(project) {
      this.load(); // Load to make sure we have the most up to date list (other tabs could have modified)
      this.projects.push(project);
      this.save();
      console.log("Added project", project);
    }

    removeProject(project) {
      this.load(); // Load to make sure we have the most up to date list (other tabs could have modified)
      this.projects = this.projects.filter((p) => p !== project);
      this.save();
      console.log("Removed project", project);
    }

    clear() {
      this.projects = [];
      this.save();
      console.log("Cleared install list");
    }

    export() {
      console.log("Exporting install list", this.projects);
      const blob = new Blob([this.projects.join("\n")], { type: "text/plain" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "sculk-install-list.txt";
      a.click();

      setTimeout(() => {
        URL.revokeObjectURL(blob);
        a.remove();
      }, 1000);

      this.clear();
    }

    save() {
      localStorage.setItem("sculk__projects", JSON.stringify(this.projects));
    }

    load() {
      const projects = localStorage.getItem("sculk__projects");
      if (projects) {
        this.projects = JSON.parse(projects);
      }
    }
  }

  class Modrinth {
    constructor(list) {
      this.list = list;
    }

    init() {
      if (MODRINTH_PATHS.find((p) => window.location.pathname.startsWith(`/${p}`))) {
        this.addButtons();
      }
    }

    addButtons() {
      const buttonDiv = document.createElement("div");
      buttonDiv.style.width = "100%";
      buttonDiv.style.display = "flex";
      buttonDiv.style.flexDirection = "column";
      buttonDiv.style.alignItems = "center";

      this.addButton = this.createButton("Add to Sculk Install List", () => this.addProject());
      buttonDiv.append(this.addButton);
      this.removeButton = this.createButton("Remove from Sculk Install List", () => this.removeProject());
      buttonDiv.append(this.removeButton);
      buttonDiv.append(this.createButton("Export Sculk Install List", () => this.export()));
      buttonDiv.append(this.createButton("Clear Sculk Install List", () => this.clear()));
      this.updateButtons();
      document.querySelector(".normal-page__sidebar").prepend(buttonDiv);
    }

    createButton(text, onclick) {
      const button = document.createElement("button");
      button.innerText = text;
      button.onclick = onclick;
      button.classList.add("iconified-button");
      button.style.width = "100%";
      button.style.marginBottom = "8px";
      return button;
    }

    addProject() {
      const slug = window.location.pathname.split("/")[2];
      this.list.addProject(`modrinth:${slug}`);
      this.updateButtons();
    }

    removeProject() {
      const slug = window.location.pathname.split("/")[2];
      this.list.removeProject(`modrinth:${slug}`);
      this.updateButtons();
    }

    export() {
      this.list.export();
      this.updateButtons();
    }

    clear() {
      this.list.clear();
      this.updateButtons();
    }

    updateButtons() {
      const slug = window.location.pathname.split("/")[2];

      if (this.list.projects.includes(`modrinth:${slug}`)) {
        this.addButton.style.display = "none";
        this.removeButton.style.display = "flex";
      } else {
        this.addButton.style.display = "flex";
        this.removeButton.style.display = "none";
      }
    }
  }

  class Curseforge {
    constructor(list) {
      this.list = list;
    }

    init() {
      if (CURSEFORGE_PATHS.find((p) => window.location.pathname.startsWith(`/minecraft/${p}`))) {
        this.addButtons();
      }
    }

    addButtons() {
      const buttonDiv = document.createElement("div");
      buttonDiv.style.width = "100%";
      buttonDiv.style.display = "flex";
      buttonDiv.style.flexDirection = "column";
      buttonDiv.style.alignItems = "center";

      this.addButton = this.createButton("Add to Sculk Install List", () => this.addProject());
      buttonDiv.append(this.addButton);
      this.removeButton = this.createButton("Remove from Sculk Install List", () => this.removeProject());
      buttonDiv.append(this.removeButton);
      buttonDiv.append(this.createButton("Export Sculk Install List", () => this.export()));
      buttonDiv.append(this.createButton("Clear Sculk Install List", () => this.clear()));
      this.updateButtons();
      document.querySelector(".tab-content").prepend(buttonDiv);
    }

    createButton(text, onclick) {
      const button = document.createElement("button");
      button.innerText = text;
      button.onclick = onclick;
      button.classList.add("btn-secondary");
      button.style.width = "100%";
      button.style.marginBottom = "8px";
      return button;
    }

    addProject() {
      const slug = window.location.pathname.split("/")[3];
      this.list.addProject(`curseforge:${slug}`);
      this.updateButtons();
    }

    removeProject() {
      const slug = window.location.pathname.split("/")[3];
      this.list.removeProject(`curseforge:${slug}`);
      this.updateButtons();
    }

    export() {
      this.list.export();
      this.updateButtons();
    }

    clear() {
      this.list.clear();
      this.updateButtons();
    }

    updateButtons() {
      const slug = window.location.pathname.split("/")[3];

      if (this.list.projects.includes(`curseforge:${slug}`)) {
        this.addButton.style.display = "none";
        this.removeButton.style.display = "flex";
      } else {
        this.addButton.style.display = "flex";
        this.removeButton.style.display = "none";
      }
    }
  }

  setTimeout(() => {
    if (window.location.hostname === MODRINTH_HOST) {
      console.log("Modrinth detected");
      const list = new List();
      const modrinth = new Modrinth(list);
      modrinth.init();
    }
    if (window.location.hostname === CURSEFORGE_HOST) {
      console.log("Curseforge detected");
      const list = new List();
      const curseforge = new Curseforge(list);
      curseforge.init();
    }
  }, 1000);
})();
