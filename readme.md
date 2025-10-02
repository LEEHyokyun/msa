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

## 5. Project Design - "DTO Seperation"

> CQRS와는 별도로 요청 유형 및 목적에 따라 명확한 DTO 구분을 위해 Request/Response 객체 분리를 진행한다.
- Create Request / Update Request / Response 등 트래픽 증가에 따라 프로젝트 복잡도가 높아지고 유지보수에 비용이 많이 들 것으로 예상할 경우, 정확한 프로젝트 관리 등을 목적으로 목적에 따른 DTO 분리를 진행한다.
- 프로젝트 규모가 클 수록 하나의 DTO로 모든 데이터 요청/응답에 대응하는 것이 어렵다.
- 요청책임을 세분화하여 관리할 수 있으며, Swagger 등 문서화 관리에도 유리하다.

## 6. Project Design - "REST API Seperation"

> 본 프로젝트의 API 설계 시 모든 API 요청을 단순 GetMapping/PostMapping 일원화가 아닌, 요청 목적 및 특징 등에 따라 PutMapping을 사용하는 API 세분화가 이루어질 수 있도록 구성
- 멱등성 유지 구분(Post : 생성/멱등성 보장 못함, Put : 수정/멱등성 보장 가능)
- URL 등 요청자원 설계 시 Post : 컬렉션(객체 자체), Put : 특정 자원(id)

## 7. Product Design - "Pagination"

> 서버의 과부하를 줄이고 성능적으로 유리한 데이터 읽기를 위해 페이징 쿼리를 활용 

[case 1) Paging]
 
- N번(=offset) 페이지에서 M개(=limit) 게시글을 불러온다.
- 총 게시글 수에 따른 총 페이지 개수를 나타낸다.

- Index를 활용하여 데이터 추출 및 페이징 성능 향상
  - [ASIS] 전체 데이터 개수가 매우 많아 row query에서 데이터 추출 시 3~4초 소요(*full scan/file sort)
  - [TOBE] Secondary Index / Clustered Index를 활용 및 적절한 전략 수립을 통해 유의미한 조회성능향상
  - [Paging Query & Index 정리](https://velog.io/@gyrbs22/%EB%B0%B1%EC%97%94%EB%93%9C-%EC%9D%B8%EB%8D%B1%EC%8A%A4%EB%A5%BC-%EB%AA%85%ED%99%95%ED%9E%88-%ED%8C%8C%EC%95%85%ED%95%98%EA%B8%B0-%EC%A1%B0%ED%9A%8C%EC%84%B1%EB%8A%A5%ED%96%A5%EC%83%81%EC%9D%B4-%EC%96%B4%EB%8A%90-%EB%B6%80%EB%B6%84%EC%97%90%EC%84%9C-%EC%9D%B4%EB%A3%A8%EC%96%B4%EC%A7%80%EB%8A%94%EA%B0%80%EB%8B%A8%EC%88%9C-%EC%9D%B8%EB%8D%B1%EC%8A%A4-%ED%99%9C%EC%9A%A9%EB%B6%80%ED%84%B0-Covering-Index-%EB%93%B1%EC%9D%98-%EC%A0%84%EB%9E%B5%EA%B9%8C%EC%A7%80)
  - 페이징 쿼리를 사용할 경우 Boot에서 제공하는 Pagable보다 JPA의 natvie query로 직접 작정하는 것이 효과적

[case 2) Infinite Scroll]

- 별도 offset 구성없이 이전에 조회한 마지막 페이지 혹은 기준점을 기억하여 다음 스크롤 시 데이터를 추출하도록 한다.
- 다음 스크롤에 나타날 데이터의 기준 혹은 추출 조건을 기억하고 Clustered Index에서 바로 데이터를 추출한다.

- Index를 활용하여 데이터 추출 및 페이징 성능 향상
  - [ASIS] offset을 찾기 위한 scan 작업과 Clustered/Secondary Index 작업을 통해 불필요한 조회비용이 발생하고, 복잡한 페이징 쿼리 작업이 필요하다.
  - [TOBE] 명확한 기준점을 매개변수로 전달하며 Clustered Index를 활용 등을 통해 조회성능향상하고 offset 관계없이 균등한 조회성능을 확보
  - 최초 무한스크롤 조회 쿼리와 이후(기준점 기억 이후) 조회 쿼리를 다르게 구성한다.

## 8. Function #1 - "comments"

> 댓글을 작성한다.
- 댓글 조회/생성/삭제 API를 구축한다.
- 댓글목록 조회 시 계층형(대댓글)으로 조회하며 오래된 순으로 각 계층별 최소 2depth부터 무한depth까지 조회한다.
- 하위 댓글이 모두 삭제되어야 상위 댓글을 삭제할 수 있으며, 하나라도 하위 댓글이 있다면 "삭제표시"만 표기된다.
- 계층형 depth에 따라 2depth = Adjaceny list(인접리스트) & 무한depth = Path enumeration(경로열거)를 사용한다.

## 9. Function #2 - "Like"

> 게시글 별 사용자는 좋아요를 표시한다.
 
[기능적 요구사항 및 설계]
- 사용자는 각각의 게시글 당 1개의 좋아요를 표시하거나 취소할 수 있다.
- 게시글 ID와 사용자 ID를 활용하여 "좋아요 테이블"을 별도 구성한다.
- 좋아요 수는 게시글 별 좋아요 생성 및 삭제 시점에 즉시 갱신
- 좋아요 테이블에서 파생 데이터(derived)를 관리하기 위한 비정규화(별도의 좋아요 수 추출)를 진행
  (*중복 집계가 발생하지 않는 상황이므로 비정규화에 어색함이 없음)

[좋아요 기능의 동시성 문제]
- [격리수준에 따른 동시성 문제 발생](https://velog.io/@gyrbs22/%EB%B0%B1%EC%97%94%EB%93%9C-MSA-%EB%B0%8F-%EB%B6%84%EC%82%B0DB-%ED%99%98%EA%B2%BD%EC%97%90%EC%84%9C-%EC%9D%98%EB%8F%84%EC%A0%81%EC%9D%B8-%ED%85%8C%EC%9D%B4%EB%B8%94-%EB%B9%84%EC%A0%95%EA%B7%9C%ED%99%94%EB%A5%BC-%ED%86%B5%ED%95%B4-%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98%EC%9D%98-%EC%84%B1%EB%8A%A5%EC%9D%84-%EB%B3%B4%EC%99%84%ED%95%A0-%EC%88%98-%EC%9E%88%EB%8A%94-%EB%B0%A9%EC%95%88%EB%8F%99%EC%8B%9C%EC%84%B1-%EB%AC%B8%EC%A0%9C%EA%B0%80-%EB%B0%9C%EC%83%9D%ED%95%98%EB%8A%94-%EC%83%81%ED%99%A9%EC%9D%84-%EA%B3%A0%EB%A0%A4%ED%95%98%EB%A9%B4%EC%84%9C)
- 데이터 일관성을 보장하기 위해 적절한 형태의 어떠한 형태의 Lock을 활용할 지 고민 필요(트랜잭션 특성상 조회에 대한 lock은 현실적으로 불가능하므로 )
- 해당 도메인의 트래픽이나 로직의 복잡도를 고려할 때 비관적 락으로도 충분히 동시성 문제 해결 가능(update 처리 시 DB row data를 그 시점에서 조회하면서 lock을 명시하는 방안)
- 현재 프로젝트에서는 모든 락 구현 방안에 대해 살펴보도록 한다.

## 9. Function #2 - "board(article)/comment count"

> 게시글 수와 댓글 수를 조회한다.
- 향후 인기글 선정을 위해 필요한 기능
- 좋아요 수와 마찬가지로 비정규화하여 별도의 테이블을 생성 및 관리한다.