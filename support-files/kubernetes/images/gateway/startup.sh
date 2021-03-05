#! /bin/sh
LOGS_DIR=/data/logs
if [ -n "$BK_REPO_LOGS_DIR" ]; then
    LOGS_DIR=$BK_REPO_LOGS_DIR
fi
mkdir -p $LOGS_DIR/nginx
chmod 777 $LOGS_DIR/nginx
##初始化配置
touch bkrepo.env
/data/workspace/render_tpl -u -p /data/workspace -m . -e bkrepo.env /data/workspace/templates/gateway*
/data/workspace/render_tpl -u -p /data/workspace -m . -e bkrepo.env /data/workspace/frontend/ui/frontend#ui#index.html

##启动程序
/usr/local/openresty/nginx/sbin/nginx -g 'daemon off;'
