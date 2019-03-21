## Run with Docker

- edit `env_vars` to set up the variables
- run: `docker run --name score-server --env-file env_vars -t overture/score-server:<tag>`

## Run in Kubernetes

- clone: `git clone https://github.com/overture-stack/charts.git`
- cd: `cd charts/score/`
- edit `values.yaml` or provide the variables with `--set`
- install helm chart: 
`helm install -n score-server .`
`helm install -n score-server --set image.tag=<tag> .`