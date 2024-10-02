FROM babashka/babashka

COPY \
	pipeline-notifier/*.clj \
	vault-exec-agent/vault-exec-agent.clj \
	\
	/scripts/
