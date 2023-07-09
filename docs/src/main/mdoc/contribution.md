# Contribution

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal contribution guide" />
  <meta name="keywords" content="ZIO Temporal contribution" />
</head>

## All suggestions welcome

See the list of issues on GitHub and pick one! Or report your own.

If you are having doubts on why or how something works, don't hesitate to ask a question on Discord or via GitHub.  
This probably means that the documentation, ScalaDocs or code is unclear and be improved for the benefit of all.  

## Testing locally
Tests doesn't require any environment installed, so simply run:
```shell
sbt test
```

**Note**: most important tests are **integration-tests**, so pay close attention when updating them.

## Building the site locally
1. Start the site in watch mode:
```shell
make start-site
```
2. In case of documentation changes, re-generate the documentation:
```shell
make gen-doc
```
The watcher should pick up your changes