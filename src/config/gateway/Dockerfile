
FROM envoyproxy/envoy-alpine:v1.13.0
COPY envoy.yaml /etc/envoy/envoy.yaml
COPY origin.key /etc/ssl/tls.key
COPY origin.crt /etc/ssl/tls.crt
EXPOSE 8443
EXPOSE 8090
EXPOSE 9901
