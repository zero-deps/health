FROM openjdk:13.0.2-jdk-oraclelinux7

MAINTAINER " PO" .po@.com

ENV LANG en_US.UTF-8

USER root

RUN yum update -y; yum install -y unzip rsync openssh-clients

RUN mkdir -p /opt/local &&\
    groupadd -g 9000 apps && \
    useradd --create-home --home-dir=/opt/local/apps --uid 9000 -g apps apps &&\
    mkdir -p /opt/local/tmp/ &&\
    chmod 777 /opt/local/tmp/

USER apps

RUN mkdir -p /opt/local/apps/-web-platform/package /opt/local/apps/-web-platform/static

COPY --chown=apps:apps ./target/universal/*zip /opt/local/apps/-web-platform/package

RUN cd /opt/local/apps/-web-platform/package && unzip *.zip -d /opt/local/apps/-web-platform/package && rm *.zip && mv stats*/* . && rm -rf stats*  && mkdir -p /opt/local/apps/-web-platform/conf &&\
    mkdir -p /opt/local/apps/-web-platform/package/appdir/logs/ &&\
    ln -sf /proc/1/fd/1 /opt/local/apps/-web-platform/package/appdir/logs/wpl.log

CMD /opt/local/apps/-web-platform/package/bin/stats -Dconfig.file=/opt/local/apps/-web-platform/conf/app.conf
