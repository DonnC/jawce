# JAWCE Example ChatBots
Example chatbot

## Setup
Before running, make sure bot config in `/src/main/resources/application.yml` are configured properly

Remember to change the base folder to suit your root folder name

If you are running on Windows OS, your config paths will be as below (if you cloned the project in Projects dir)
```yaml
resources:
  templates: C:\\Projects\\jawce\\example\\jchatbot\\src\\main\\resources\\templates
  triggers:  C:\\Projects\\jawce\\example\\jchatbot\\src\\main\\resources\\triggers
  watcher:   C:\\Projects\\jawce\\example\\jchatbot\\src\\main\\resources\\watch
  hooks:
    ...
```

If you are running on Unix OS, your config paths will be as below
```yaml
resources:
  templates: /home/projects/jawce/example/jchatbot/src/main/resources/templates
  triggers:  /home/projects/jawce/example/jchatbot/src/main/resources/triggers
  watcher:   /home/projects/jawce/example/jchatbot/src/main/resources/watch
  hooks:
    ...
```
