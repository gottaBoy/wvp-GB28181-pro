FROM ubuntu:20.04 AS builder

# 设置非交互式安装和时区
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

RUN apt-get update && \
    apt-get install -y nodejs npm  && \
    rm -rf /var/lib/apt/lists/*

COPY ./web /build
WORKDIR /build

RUN npm --registry=https://registry.npmmirror.com install
RUN npm run build:prod

WORKDIR /src/main/resources
RUN ls

WORKDIR /src/main/resources/static
RUN ls

FROM harbor.intra.zeron.ai/library/nginx:alpine3.20-slim

ARG TZ=Asia/Shanghai


COPY --from=builder /src/main/resources/static /opt/dist

CMD ["nginx","-g","daemon off;"]

