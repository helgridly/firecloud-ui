#!/bin/bash
set -euo pipefail
IFS=$'\n\t'


SITE_CONF=$(cat << EOF
ServerAdmin ${SERVER_ADMIN}
ServerName ${SERVER_NAME}

LogFormat "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\"" combined
LogLevel ${LOG_LEVEL}

<VirtualHost _default_:${HTTPD_PORT}>
  ErrorLog /dev/stdout
  CustomLog "/dev/stdout" combined

  # Allow for proxying requests to HTTPS endpoints.
  SSLProxyEngine on

  --EXTRA_VHOST_HTTP--
</VirtualHost>

<VirtualHost _default_:${SSL_HTTPD_PORT}>
  ErrorLog /dev/stdout
  CustomLog "/dev/stdout" combined

  SSLEngine on
  SSLProxyEngine on
  SSLCertificateFile "/etc/ssl/certs/server.crt"
  SSLCertificateKeyFile "/etc/ssl/private/server.key"
  SSLCertificateChainFile "/etc/ssl/certs/ca-bundle.crt"

  --EXTRA_VHOST_HTTPS--
</VirtualHost>

DocumentRoot /app/target

<Directory "/app/target">
  AllowOverride All
  Order allow,deny
  Allow from all
  Require all granted
</Directory>
EOF
)

LOCATION_DIRECTIVES=$(cat << EOF
  <LocationMatch "/service/api/workspaces/[^/]+/[^/]+/entities/[^/]+/tsv">
    RewriteEngine On
    RewriteCond %{HTTP_COOKIE} FCtoken=([^;]+)
    RewriteRule .* - [END,QSD,env=ACCESSTOKEN:%1]
    RequestHeader set Authorization "Bearer %{ACCESSTOKEN}e"
    ProxyPass ${ORCH_URL_ROOT}
    ProxyPassReverse ${ORCH_URL_ROOT}
  </LocationMatch>

  <Location "/service">
    ProxyPass ${ORCH_URL_ROOT}
    ProxyPassReverse ${ORCH_URL_ROOT}
  </Location>
EOF
)

if [ "$HTTPS_ONLY" = 'true' ]; then
  SITE_CONF=${SITE_CONF/'--EXTRA_VHOST_HTTP--'/'Redirect / '"https://$SERVER_NAME/"}
else
  SITE_CONF=${SITE_CONF/'--EXTRA_VHOST_HTTP--'/"$LOCATION_DIRECTIVES"}
fi

SITE_CONF=${SITE_CONF/'--EXTRA_VHOST_HTTPS--'/"$LOCATION_DIRECTIVES"}

set -x

echo "$SITE_CONF" > /etc/apache2/sites-available/site.conf
exec /usr/sbin/apachectl -DNO_DETACH -DFOREGROUND 2>&1
