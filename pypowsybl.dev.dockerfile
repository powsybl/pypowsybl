FROM python:3.7-bookworm

RUN apt-get update && apt-get install -y \
    cmake \
    gcc \
 && rm -rf /var/lib/apt/lists/*

RUN curl -O https://download.oracle.com/graalvm/17/latest/graalvm-jdk-17_linux-x64_bin.tar.gz
RUN tar xzf graalvm-jdk-17_linux-x64_bin.tar.gz
RUN mv graalvm-jdk-17.0.7+8.1 /usr/local/bin/

ENV JAVA_HOME /usr/local/bin/graalvm-jdk-17.0.7+8.1/

RUN curl -O https://dlcdn.apache.org/maven/maven-3/3.9.3/binaries/apache-maven-3.9.3-bin.zip
RUN unzip apache-maven-3.9.3-bin.zip
RUN mv apache-maven-3.9.3/ /usr/local/bin/
ENV PATH="${PATH}:/usr/local/bin/apache-maven-3.9.3/bin"

ADD . /pypowsybl

WORKDIR /pypowsybl

RUN rm -rf build

RUN pip install --upgrade setuptools pip
RUN pip install -r requirements.txt
RUN pip install .
