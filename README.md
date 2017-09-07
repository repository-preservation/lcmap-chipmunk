# Chipmunk

Chipmunk. It's nuts.

## Deploying Chipmunk

Chipmunk is run as a Docker container so you don't have to worry
about installing GDAL or building an uberjar.

```
export DB_HOST=<your_cassandra_host_name>
export HTTP_PORT=5858
docker run -p 5858:5858 -it usgseros/lcmap-chipmunk:latest
```

Use these environment variables to

| ENV            | Description                 |
| -------------- | --------------------------- |
| `DB_HOST`      | List of Cassandra nodes     |
| `DB_USER`      | Cassandra username          |
| `DB_PASS`      | Cassandra password          |
| `DB_PORT`      | Cassandra password          |
| `DB_KEYSPACE`  | Chipmunk's keyspace name    |
| `HTTP_PORT`    | HTTP server listening port  |


## Developing Chipmunk

You need to install a few dependencies before running Chipmunk locally.

* [Git Large File Storage](https://git-lfs.github.com/).
* [Docker Compose](https://docs.docker.com/compose/install/)
* [GDAL 1.11.x](https://gdal.org)

### Checkout the repository

```
git clone git@github.com:USGS-EROS/lcmap-chipmunk.git
cd lcmap-chipmunk
```

Start backing services with Docker compose.

```
make docker-compose-up
```

Create a `profiles.clj` so you don't have to mess with environment
variables for local development. The built-in example will work
out-of-the-box.

```
cp profiles.example.clj profiles.clj
```

Run the tests.

```
lein test
```

Start a REPL.

```
lein repl
```

## Environment Variables


## Conclusion

Chipmunk. It's nuts.
