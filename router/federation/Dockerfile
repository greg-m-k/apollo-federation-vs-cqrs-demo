FROM ghcr.io/apollographql/router:v1.57.1

# Copy router configuration
COPY router.yaml /dist/config/router.yaml

# Copy supergraph schema (will be composed at build time)
COPY supergraph.graphql /dist/config/supergraph.graphql

# Expose ports
EXPOSE 4000 8088 9090

# Health check
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8088/health || exit 1

# Run the router
CMD ["--config", "/dist/config/router.yaml", "--supergraph", "/dist/config/supergraph.graphql"]
