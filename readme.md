## 1.  개요
> Study
- 본 프로젝트는 MSA Project 설계 및 구현 등에 대한 공부 내용을 담고 있으며, 실전에 바로 투입할 수 있을 정도의 깊이로 학습한 내용을 반영한다.
> Basic Structure
- 향후 개인 혹은 팀 프로젝트 설계 시 MSA / Monolithic 등 프로젝트 구조(유형) 상관없이 본 프로젝트를 최대한 활용할 수 있을 것에 염두를 둔다. 

## 2. Proejct Configuration - "Basic"

> 독립적인 마이크로서비스의 구동을 위해 빌드(settings.gradle) 등 모든 설정은 별도로 진행
- 각 MicroServices Modules들은 kuke-board라는 root project 최상위 Module에 속하는 하위 Modules이다.
- Module에 대한 gradle 설정은 따로 진행하며, 상위 모듈에서 include하여 이를 반영하는 구조이다.
- java와 test 역시 각 Module 내에서 따로 구성해주도록 한다.
- 각 Module의 독립적인 실행을 보장하기 위해 SpringBootApplication(실행파일)을 각 Module 별로 따로 구성한다.
- MicroServices의 실행 포트는 모두 다르며, Event 혹은 Message를 통해 유기적인 상호작용을 한다.

## 3. Project Design - "BRD"

> 게시글을 작성하기 위한 테이블을 설계하며 대용량 트래픽 상황을 가정하여 게시글을 N개의 샤드로 샤딩하는 환경을 가정
- Primary Key = Article Id.
- 동일 카테고리에 속한 게시글들이 방대해질 경우 이를 샤딩하는 상황을 가정하며, 샤딩의 효율성을 고려함에 따라 Shard Key = Board Id.
- 샤딩기법은 편의상 Hash-Based Sharding으로 가정한다.

## 4. Proejct Design - "PK"

> PK채번을 위해 분산환경에서 순차성과 고유성을 보장할 수 있는 채번 알고리즘을 활용한다.
- X에서 제공하는 Snowflake 알고리즘을 사용하여 위 요구사항을 만족할 수 있도록 PK채번을 진행한다.
- Snowflake을 활용한 채번은 공통활용모듈로 Common 도메인 책임 하에 둔다.