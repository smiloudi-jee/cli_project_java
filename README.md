# MCP Chat (Java)

Conversion Java de l'exercice Python "MCP Chat" (cours *Introduction to Model Context Protocol*).

Application en ligne de commande permettant de discuter avec un modele Claude via l'API Anthropic, avec recuperation de documents, prompts commandes et outils exposes via un serveur MCP (Model Context Protocol).

## Prerequis

- JDK 17+
- Maven 3.9+
- Une cle API Anthropic

## Mise en place

### Etape 1 : variables d'environnement

Editez le fichier `.env` a la racine du projet :

```
CLAUDE_MODEL="claude-sonnet-4-5"
ANTHROPIC_API_KEY=""   # Renseignez votre cle secrete Anthropic
```

### Etape 2 : compiler le projet

```
mvn clean compile
```

### Etape 3 : lancer le projet

```
mvn exec:java ou mvn clean compile exec:java (si l'étape 2 n'a pas été exécuté)
```
---

#### Important : 

Utilisez bien `mvn clean compile exec:java` (et non `mvn exec:java` seul).

Le serveur MCP applicatif est lancé comme sous-processus Java, dont le classpath
est materialise dans `target/classpath.txt` via le plugin `maven-dependency-plugin` qui
execute a la phase `generate-sources`. 

Laquelle ne s'execute que si `compile` (ou une phase posterieure) fait partie de la commande.

Pour plus de détails, voir les commentaires du fichier pom.xml

---

## Utilisation

### Interaction de base

Une fois l'application lancée, tapez simplement votre message et appuyez sur Entree pour discuter avec le modele.

### Recuperation de documents

Utilisez le symbole `@` suivi d'un identifiant de document pour inclure son contenu dans votre requête :

```
> Tell me about @deposition.md
```

### Commandes

Utilisez le prefixe `/` pour executer une commande definie par le serveur MCP :

```
> /summarize deposition.md
```

Les commandes s'auto-completent quand vous appuyez sur Tab (via JLine, l'equivalent Java de prompt-toolkit).

## Structure du projet

```
cli_project_java/
├── pom.xml
├── .env
├── src/main/java/com/formation/mcpchat/
│   ├── Main.java              (equivalent de main.py)
│   ├── MCPClient.java         (equivalent de mcp_client.py — TODOs a completer)
│   ├── McpServerApp.java      (equivalent de mcp_server.py — TODOs a completer)
│   └── core/
│       ├── Claude.java        (equivalent de core/claude.py)
│       ├── Chat.java          (equivalent de core/chat.py)
│       ├── CliChat.java       (equivalent de core/cli_chat.py)
│       ├── ToolManager.java   (equivalent de core/tools.py)
│       └── Cli.java           (equivalent de core/cli.py)
```

## Developpement

### Ajouter de nouveaux documents

Editez `McpServerApp.java` pour ajouter des documents dans la Map `DOCS`.

### Implementer les fonctionnalites MCP

Pour terminer l'implementation du protocole MCP :

1. Completez les TODOs dans `McpServerApp.java` (outils, resources, prompts cote serveur)
2. Completez les TODOs dans `MCPClient.java` (appels au serveur cote client)

### Dependances principales

- `com.anthropic:anthropic-java` — SDK Java officiel pour l'API Claude
- `io.modelcontextprotocol.sdk:mcp` — SDK Java officiel du Model Context Protocol
- `org.jline:jline` — boucle CLI interactive avec auto-completion
- `io.github.cdimascio:dotenv-java` — chargement du fichier `.env`

### Lint et verification de types

Aucun outil de lint/format n'est configure (equivalent au projet Python d'origine). La verification de types est assuree nativement par le compilateur Java.

## Notes sur la conversion depuis Python

- Le typage dynamique de Python (`Any`, dictionnaires bruts pour les messages) a ete remplace par les types forts du SDK Java Anthropic/MCP (`MessageParam`, `McpSchema.Tool`, `ReadResourceResult`, etc.).
- prompt-toolkit n'existe pas en Java : la boucle interactive et l'auto-completion `/` et `@` sont reproduites avec **JLine**.
- python-dotenv est remplacé par **dotenv-java**.
- Le serveur MCP (`McpServerApp`) est lancé comme sous-processus Java avec son classpath materialise dans `target/classpath.txt`, ce qui évite d'avoir à gérer deux artefacts sépares (equivalent de `uv run mcp_server.py`).
