# Chipmunk

Chipmunk. It's nuts.

## Deploying Chipmunk

Chipmunk is run as a Docker container so you don't have to worry
about installing GDAL or building an uberjar. It will automatically
create keyspaces and default tables if you give it priveleged
credentials.

```
export DB_HOST=<your_cassandra_host_name>
export HTTP_PORT=5858
docker run -p 5858:5858 -it usgseros/lcmap-chipmunk:latest
```

Chipmunk is configured using these environment variables:

| ENV            | Description                 |
| -------------- | --------------------------- |
| `HTTP_PORT`    | Chipmunk's HTTP listener    |
| `DB_HOST`      | Cassandra node (just one)   |
| `DB_USER`      | Cassandra username          |
| `DB_PASS`      | Cassandra password          |
| `DB_PORT`      | Cassandra cluster port      |
| `DB_KEYSPACE`  | Chipmunk's keyspace name    |


## Developing Chipmunk

You need to install a few dependencies before running Chipmunk locally.

* [Docker Compose](https://docs.docker.com/compose/install/)
* [GDAL 1.11.x](https://gdal.org)

### Checkout the repository

```
git clone git@github.com:USGS-EROS/lcmap-chipmunk.git
cd lcmap-chipmunk
```

Start backing services using Docker compose.

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

## Conclusion

Chipmunk. It's nuts.
