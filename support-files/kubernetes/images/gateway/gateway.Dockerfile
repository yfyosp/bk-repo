FROM blueking/openresty:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

COPY ./ /data/workspace/


RUN ls -l /usr/local/openresty/nginx

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    rm -rf /usr/local/openresty/nginx/conf &&\
    ln -s  /data/workspace/gateway /usr/local/openresty/nginx/conf &&\
    ln -s  /data/workspace/frontend /usr/local/openresty/nginx/conf &&\
    mkdir -p /usr/local/openresty/nginx/run/ &&\
    chmod +x /data/workspace/startup.sh &&\
    chmod +x /data/workspace/render_tpl

RUN ls -l /usr/local/openresty/nginx
