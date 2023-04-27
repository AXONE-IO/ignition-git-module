# Docker compose guide

### Prerequisites :
- Docker : https://www.docker.com/
- Configure .env file (especially with the right COMPOSE_FILE)

### Classic Docker Compose :
- Put git module in ./modules/ folder
- Fill ./gw-ini/git.conf with right configurations
- Fill the ./gw-secrets/GATEWAY_ADMIN_PASSWORD file with right password
- Fill the ./gw-secrets/GATEWAY_GIT_USER_SECRET file with the right password or ssh key relative to git.conf (not necessary if the password is directly filled in git.conf, but less secure)
- Modify the docker-compose to your liking
- Run the command line "docker compose up"

### Classic Docker Compose full automated (Derived Image Solution) :
Based on : https://github.com/thirdgen88/ignition-derived-example
- Fill the right version of git module in ./gw-build/Dockerfile
    - SUPPLEMENTAL_GIT_DOWNLOAD_URL
- Fill ./gw-ini/git.conf with right configurations
- Fill the ./gw-secrets/GATEWAY_ADMIN_PASSWORD file with right password
- Fill the ./gw-secrets/GATEWAY_GIT_USER_SECRET file with the right password or ssh key relative to git.conf (not necessary if the password is directly filled in git.conf, but less secure)
- Modify the docker-compose to your liking
- Run the command line "docker compose up"
