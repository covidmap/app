
FROM envoyproxy/envoy-alpine:v1.13.0
COPY envoy.yaml /etc/envoy/envoy.yaml
COPY sandbox.key /etc/ssl/tls.key
COPY sandbox.crt /etc/ssl/tls.crt
EXPOSE 8443
EXPOSE 8090
EXPOSE 9901
