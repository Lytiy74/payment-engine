sudo dnf install -y java-21-openjdk-devel git tmux
java -version
uname -m
git clone ~projects/payment-engine
./mvnw -B verify
