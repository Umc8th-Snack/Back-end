FROM python:3.11-slim

# 필수 패키지 설치
RUN apt-get update && apt-get install -y \
    curl git make build-essential \
    autoconf automake libtool \
    pkg-config zlib1g-dev python3-dev \
    g++ \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 작업 디렉토리
WORKDIR /opt

# mecab 설치
RUN git clone --depth 1 https://github.com/konlpy/mecab-ko-docker.git && \
    cd mecab-ko-docker && ./install_mecab-ko.sh && \
    cd .. && rm -rf mecab-ko-docker


# konlpy, mecab-python 설치
WORKDIR /app
COPY python-nlp-service/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY python-nlp-service/ .

CMD ["python", "bootstrap.py"]
