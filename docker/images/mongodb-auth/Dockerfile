FROM mongo:8.0

LABEL maintainer="<Enrico Russo> enrico.russo.84@gmail.com"

COPY keyfile /etc/keyfile
COPY run.sh /run.sh
COPY configure.sh /configure.sh

RUN chmod 400 /etc/keyfile; \
    chown mongodb:mongodb /etc/keyfile; \
    chmod +x /run.sh; \
    chmod +x /configure.sh

CMD ["/run.sh"]
