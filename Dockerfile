FROM babashka/babashka

COPY \
	pipeline-notifier/gitlab2teams.clj \
	vault-exec-agent/vault-exec-agent.clj \
	\
	/scripts/
