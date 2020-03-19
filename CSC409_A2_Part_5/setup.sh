# Export config variables as ENV variables
source config.conf
export $(cut -d= -f1 config.conf)

# Make a new swarm instance
docker swarm leave --force
docker swarm init --advertise-addr $ADVERTISE_ADDR

# Login to our FAAS system
cd $PATH_TO_FAAS_FOLDER
"./deploy_stack.sh" --no-auth

# Give it time to setup
sleep 5s

# Build and deploy our services
cd $PATH_TO_A2_FOLDER
"./buildAndDeploy.sh"
