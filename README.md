# README

## Generate certs

```
mkdir -p src/main/resources/combi
sh ./gen-certs.sh
```

## Start server

```
sbt "runMain com.example.quickstart.Main true"
```

## Run client

```
sbt "runMain com.example.quickstart.ClientExample"
```