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

(function () {
  "use strict";

  class List {
    constructor() {
      this.projects = [];
      this.load();
    }

    addProject(project) {
			this.projects.push(project);
			this.save();
      console.log("Added project", project);
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
      GM_setValue("projects", JSON.stringify(this.projects));
    }

    load() {
      const projects = GM_getValue("projects");
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
      if (window.location.pathname.startsWith("/mod/")) {
        this.addButtons();
      }
    }

    addButtons() {
      const buttonDiv = document.createElement("div");
      buttonDiv.style.width = "100%";
      buttonDiv.style.display = "flex";
      buttonDiv.style.flexDirection = "column";
      buttonDiv.style.alignItems = "center";
      buttonDiv.append(this.createButton("Add to Sculk Install List", () => this.addProject()));
      buttonDiv.append(this.createButton("Export Sculk Install List", () => this.list.export()));
      buttonDiv.append(this.createButton("Clear Sculk Install List", () => this.list.clear()));
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
    }
	}
	
	class Curseforge {
    constructor(list) {
      this.list = list;
    }

    init() {
      if (window.location.pathname.startsWith("/minecraft/")) {
        this.addButtons();
      }
    }

    addButtons() {
      const buttonDiv = document.createElement("div");
      buttonDiv.style.width = "100%";
      buttonDiv.style.display = "flex";
      buttonDiv.style.flexDirection = "column";
      buttonDiv.style.alignItems = "center";
      buttonDiv.append(this.createButton("Add to Sculk Install List", () => this.addProject()));
      buttonDiv.append(this.createButton("Export Sculk Install List", () => this.list.export()));
      buttonDiv.append(this.createButton("Clear Sculk Install List", () => this.list.clear()));
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
