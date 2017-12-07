# Chipmunk

Chipmunk enables time-series access to geospatial raster data.

You can use it to define, store, and retrieve collections of data
over HTTP.


## Deploying

Chipmunk is run as a Docker container so you don't have to worry
about installing GDAL or building an uberjar. It will automatically
create keyspaces and default tables if you give it priveleged
credentials.

```
export DB_HOST=<your_cassandra_host_name>
export HTTP_PORT=5656
docker run -p 5656:5656 -it usgseros/lcmap-chipmunk:latest
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


## Presets for Layers and Grids

Once you have a Chipmunk instance running, you may want to load it with
some data. Please install [HTTPie][2] before using them.


Predefined layers and grids can be loaded by running:

```
./bin/setup
```

Sample data can be downloaded by running:

```
./bin/download-data
```

After downloading sample data, ingest it all like this:

```
ls test/nginx/data/**/*.tar | xargs bin/load
```

The `bin/load` script generates full URLs to individual GeoTIFFs contained
in tar files and handles building URLs to the files hosted on the provided
NGINX server that can be retrieved by a Chipmunk instance running as a Docker
container.


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

Configure the registry and grids.

```
./bin/setup
```

### Additional Data

You may find it desirable to retrieve additional data to help you
become more familiar with Chipmunk. Although data can be ingested
directly from non-local sources, bandwidth constraints and rate
limits may prompt you to host your own data for ingest locally.

Download Landsat ARD for tile H003V009. Feel free to edit this script to
obtain other data. (This script skips files that have already been
retrieved).

```
./bin/download-data
```

Once retrieved, the data can be ingested using the following:

```
ls test/nginx/data/**/*.tar | xargs bin/load
```

## Resources


### Registry

The `/registry` resource provides data about the collections of data
contained by a Chipmunk instance.

Get info about all layers in a Chipmunk instance:

```
http localhost:5656/registry
```

You may also query the registry using one or more `tags` parameters to
get a subset of layers.

```
http localhost:5656/registry \
     tags==blue \
     tags==sr
```

### Grids

Chipmunk instances use one or more grids to calculate points at a
regular interval.

A grid provides parameters that can be used with an RST (rotation,
scaling, tranformation) matrix.

Get all defined grids.

```
http GET localhost:5656/grid
```

Get the grid point for the unit that 'contains' the given point.

```
http GET localhost:5656/grid/snap x==1631415 y==1829805
```

Get the grid points for units surrounding (and including) the given point.

```
http GET localhost:5656/grid/near x==1631415 y==1829805
```


### Inventory

The `/inventory` resource is used to add raster data (using an HTTP POST
request) or to retrieve info about what was ingested (using an HTTP GET).

To ingest data, POST a JSON document containing the URL to the raster data.

Please Note: If you are running Chipmunk inside a Docker container, you
must use the IP address of the NGINX server. This can be obtained and set
for later use by running:

```
export NGINX_HOST=`docker inspect -f "{{ .NetworkSettings.Networks.resources_lcmap_chipmunk.IPAddress }}" resources_nginx_1`
```

```
http POST localhost:5656/inventory \
     url=http://$NGINX_HOST/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif
```

The response contains detailed information about the source and a list
of the chips that were extracted.

To retrieve this information again, perform a `GET` using the same URL.

```
http GET localhost:5656/inventory \
     url==http://$NGINX_HOST/LC08_CU_027009_20130701_20170729_C01_V01_SR.tar/LC08_CU_027009_20130701_20170729_C01_V01_SRB1.tif
```


### Chips

The `/chips` resource provides a way to retrieve raw raster data. It gets
a list of chips, encoded as JSON, in response to spatio-temporal queries.

Please note: the `ubid` parameter corresponds to the name of a layer
defined in the registry. These parameter names are selected to maintain
compatability with [Merlin][1].

```
http GET localhost:5656/chips \
     ubid==lc08_srb1          \
     x==1631415               \
     y==1829805               \
     acquired==1980/2020
```


## Merlin Compatability Notes

_TODO: Not implemented, yet._

Backward compatability with [Merlin][1] will be provided via two resources:

* `/merlin/chips`
* `/merlin/chip-specs`

Currently, Merlin specific changes are mixed into the entire codebase. This
needs to be isolated so that consistent terms (layer vs. chip-spec, name vs.
ubid) can be used.



[1]: https://github.com/USGS-EROS/lcmap-merlin
[2]: https://httpie.org/#installation