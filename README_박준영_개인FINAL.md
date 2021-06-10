![image](https://user-images.githubusercontent.com/15603058/119284989-fefe2580-bc7b-11eb-99ca-7a9e4183c16f.jpg)

# 숙소예약(AirBnB)

본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


# Table of contents

- [예제 - 숙소예약](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

AirBnB 커버하기

기능적 요구사항
1. 호스트가 임대할 숙소를 등록/수정/삭제한다.
2. 고객이 숙소를 선택하여 예약한다.
3. 예약과 동시에 결제가 진행된다.
4. 예약이 되면 예약과 마일리지 적립 내역(Message)이 전달된다. -- 개인 Final
5. 고객이 예약을 취소할 수 있다.
6. 예약 사항이 취소될 경우 취소와 마일리지 감소 내역(Message)이 전달된다. -- 개인 Final
7. 숙소에 후기(review)를 남길 수 있다.
8. 전체적인 숙소에 대한 정보 및 예약 상태, 적립 상태 등을 한 화면에서 확인 할 수 있다.(viewpage) -- 개인 Final
9. 결재가 되면 마일리지(Mileage) 가 적립된다. -- 개인 Final
10. 예약이 취소될 경우 마일리지도 감소한다. -- 개인 Final

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 예약 건은 성립되지 않아야 한다.  (Sync 호출)
1. 장애격리
    1. 숙소 등록 및 메시지 전송 기능이 수행되지 않더라도 예약은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 예약 시스템이 과중되면 사용자를 잠시동안 받지 않고 잠시 후에 하도록 유도한다  Circuit breaker, fallback
1. 성능
    1. 모든 방에 대한 정보 및 예약 상태 등을 한번에 확인할 수 있어야 한다  (CQRS)
    1. 예약의 상태가 바뀔 때마다 메시지로 알림을 줄 수 있어야 한다  (Event driven)


# 체크포인트

- 분석 설계

  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/77129832/119316165-96ca3680-bcb1-11eb-9a91-f2b627890bab.png)

## TO-BE 조직 (Vertically-Aligned)  
  ![image](https://user-images.githubusercontent.com/77129832/119315258-a09f6a00-bcb0-11eb-9940-c2a82f2f7d09.png)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/QtpQtDiH1Je3wad2QxZUJVvnLzO2/share/6f36e16efdf8c872da3855fedf7f3ea9


### 이벤트 도출
![image](https://user-images.githubusercontent.com/15603058/121187810-697dab00-c8a3-11eb-87fc-224972b70494.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/15603058/121187996-9d58d080-c8a3-11eb-8a93-0cb804043af0.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
        - 등록시>RoomSearched, 예약시>RoomSelected :  UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외
	- MileageModified : Increased, Decreased 로 구분되어 사용되는 중복된 이벤트로 제외

### 액터, 커맨드 부착하여 읽기 좋게
![image](https://user-images.githubusercontent.com/15603058/121188970-967e8d80-c8a4-11eb-9d17-b0f062be3dc2.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/15603058/121189332-f6753400-c8a4-11eb-9906-009cb580a188.png)

    - Room, Reservation, Payment, Review 은 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌
    - 신규 개인과제로 Mileage 에 대해 트랜잭션이 유지되어야하는 단위로 묶어줌

### 바운디드 컨텍스트로 묶기

![image](https://user-images.githubusercontent.com/15603058/121190107-af3b7300-c8a5-11eb-8e3e-73f86d27ad18.png)

    - 도메인 서열 분리 
        - Core Domain:  reservation, room : 없어서는 안될 핵심 서비스이며, 연간 Up-time SLA 수준을 99.999% 목표, 배포주기는 reservation 의 경우 1주일 1회 미만, room 의 경우 1개월 1회 미만
        - Supporting Domain:   message, viewpage, mileage : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
        - General Domain:   payment : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음 

### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)

![image](https://user-images.githubusercontent.com/15603058/121191131-a303e580-c8a6-11eb-8caa-60dd7e582ae1.png)


### 폴리시의 이동과 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)

![image](https://user-images.githubusercontent.com/15603058/121192325-cbd8aa80-c8a7-11eb-8111-47ae366b5d43.png)

### 완성된 1차 모형

![image](https://user-images.githubusercontent.com/15603058/121192531-00e4fd00-c8a8-11eb-9862-d9f6a97de927.png)

    - View Model 추가

### 1차 완성본에 대한 기능적/비기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/15603058/121193072-7d77db80-c8a8-11eb-90fc-b5feed967274.png)

    - 호스트가 임대할 숙소를 등록/수정/삭제한다.(ok)
    - 고객이 숙소를 선택하여 예약한다.(ok)
    - 예약과 동시에 결제가 진행된다.(ok)
    - 예약이 되면 예약과 마일리지 적립 내역(Message)이 전달된다.(?)
    - 고객이 예약을 취소할 수 있다.(ok)
    - 예약 사항이 취소될 경우 취소와 마일리지 감소 내역(Message)이 전달된다.(?)
    - 숙소에 후기(review)를 남길 수 있다.(ok)
    - 전체적인 숙소에 대한 정보 및 예약 상태, 적립 상태 등을 한 화면에서 확인 할 수 있다.(View-green Sticker 추가로 ok)
    - 결재가 되면 마일리지(Mileage) 가 적립된다.(ok)
    - 취소가 되면 마일리지가 감소된다.(ok)
    
### 모델 수정

![image](https://user-images.githubusercontent.com/15603058/121446207-fe7cc300-c9cd-11eb-8a28-80992f9c6d14.png)

    - 수정된 모델은 모든 요구사항을 커버함.

### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/15603058/121194265-8f0db300-c8a9-11eb-855d-99f41a09abdc.png)

- 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
- 결제 후 적립 처리: 결제가 완료되야 마일리지가 적립되어, ACID 트랜잭션 적용. 마일리지 적립에 대해서는 Request-Response 방식 처리
- 결제 완료시 Host 연결 및 예약처리:  reservation 에서 room 마이크로서비스로 예약요청이 전달되는 과정에 있어서 room 마이크로 서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
- 나머지 모든 inter-microservice 트랜잭션: 예약상태, 후기처리 등 모든 이벤트에 대해 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.


## 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/80744273/119319091-fc6bf200-bcb4-11eb-9dac-0995c84a82e0.png)


    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
   mvn spring-boot:run
```

## CQRS

숙소(Room) 의 사용가능 여부, 리뷰 및 예약/결재 등 총 Status 에 대하여 고객(Customer)이 조회 할 수 있도록 CQRS 로 구현하였다.
- room, review, reservation, payment 개별 Aggregate Status 를 통합 조회하여 성능 Issue 를 사전에 예방할 수 있다.
- 비동기식으로 처리되어 발행된 이벤트 기반 Kafka 를 통해 수신/처리 되어 별도 Table 에 관리한다
- Table 모델링 (ROOMVIEW)
![image](https://user-images.githubusercontent.com/15603058/121321665-46a5d200-c949-11eb-924a-984802445406.png)

- viewpage MSA ViewHandler 를 통해 구현 ("RoomRegistered" 이벤트 발생 시, Pub/Sub 기반으로 별도 Roomview 테이블에 저장)
  ![image](https://user-images.githubusercontent.com/15603058/121322340-d6e41700-c949-11eb-86e6-6753cd5b1285.png)
  ![image](https://user-images.githubusercontent.com/15603058/121322512-fe3ae400-c949-11eb-93b5-b37477ca5506.png)
- 실제로 view 페이지를 조회해 보면 모든 room에 대한 전반적인 예약 상태, 결제 상태, 리뷰 건수 등의 정보를 종합적으로 알 수 있다
  ![image](https://user-images.githubusercontent.com/15603058/121326022-1a8c5000-c94d-11eb-8908-e0d30ad72cf3.png)


## API 게이트웨이
      1. gateway 스프링부트 App을 추가 후 application.yaml내에 각 마이크로 서비스의 routes 를 추가하고 gateway 서버의 포트를 8080 으로 설정함
         개인 Final 에서는 mileage 부분만 
       
          - application.yaml 예시
            ```
            spring:
              profiles: docker
              cloud:
                gateway:
                  routes:
                    - id: payment
                      uri: http://payment:8080
                      predicates:
                        - Path=/payments/** 
                    - id: room
                      uri: http://room:8080
                      predicates:
                        - Path=/rooms/**, /reviews/**, /check/**
                    - id: reservation
                      uri: http://reservation:8080
                      predicates:
                        - Path=/reservations/**
                    - id: message
                      uri: http://message:8080
                      predicates:
                        - Path=/messages/** 
                    - id: viewpage
                      uri: http://viewpage:8080
                      predicates:
                        - Path= /roomviews/**
		    - id: mileage
		      uri: http://mileage:8080
          	      predicates:
            	        - Path= /mileages/**
                  globalcors:
                    corsConfigurations:
                      '[/**]':
                        allowedOrigins:
                          - "*"
                        allowedMethods:
                          - "*"
                        allowedHeaders:
                          - "*"
                        allowCredentials: true

            server:
              port: 8080            
            ```

         
      2. Kubernetes용 Deployment.yaml 을 작성하고 Kubernetes에 Deploy를 생성함
          - Deployment.yaml 예시
          

            ```
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: gateway
              namespace: airbnb
              labels:
                app: gateway
            spec:
              replicas: 1
              selector:
                matchLabels:
                  app: gateway
              template:
                metadata:
                  labels:
                    app: gateway
                spec:
                  containers:
                    - name: gateway
                      image: 879772956301.dkr.ecr.us-east-2.amazonaws.com/gateway:1.0
                      ports:
                        - containerPort: 8080
            ```               
            

            ```
            Deploy 생성
            kubectl apply -f deployment.yaml
            ```     
          - Kubernetes에 생성된 Deploy. 확인
            
![image](https://user-images.githubusercontent.com/15603058/121323280-a8b30700-c94a-11eb-8c83-5cfb278a36e8.png)
	    
            
      3. Kubernetes용 Service.yaml을 작성하고 Kubernetes에 Service/LoadBalancer을 생성하여 Gateway 엔드포인트를 확인함. 
          - Service.yaml 예시
          
            ```
            apiVersion: v1
              kind: Service
              metadata:
                name: gateway
                namespace: airbnb
                labels:
                  app: gateway
              spec:
                ports:
                  - port: 8080
                    targetPort: 8080
                selector:
                  app: gateway
                type:
                  LoadBalancer           
            ```             

           
            ```
            Service 생성
            kubectl apply -f service.yaml            
            ```             
            
            
          - API Gateay 엔드포인트 확인
           
            ```
            Service  및 엔드포인트 확인 
            kubectl get svc -n airbnb           
            ```                 
![image](https://user-images.githubusercontent.com/15603058/121323324-b1a3d880-c94a-11eb-87d0-191f3a4328c7.png)

# Correlation

Airbnb 프로젝트에서는 PolicyHandler에서 처리 시 어떤 건에 대한 처리인지를 구별하기 위한 Correlation-key 구현을 
이벤트 클래스 안의 변수로 전달받아 서비스간 연관된 처리를 정확하게 구현하고 있습니다. 

아래의 구현 예제를 보면

예약(Reservation)을 하면 동시에 연관된 방(Room), 결제(Payment), 마일리지(Mileage) 등의 서비스의 상태가 적당하게 변경이 되고,
예약건의 취소를 수행하면 다시 연관된 방(Room), 결제(Payment), 마일리지(Mileage) 등의 서비스의 상태값 등의 데이터가 적당한 상태로 변경되는 것을
확인할 수 있습니다.

방, 예약은 팀 Final 시 완료하여 캡처 생략

예약등록
![image](https://user-images.githubusercontent.com/15603058/121325627-b6698c00-c94c-11eb-8fac-667735d3511a.png)
예약 후 - 결제 상태
![image](https://user-images.githubusercontent.com/15603058/121325666-c1bcb780-c94c-11eb-998f-c7ab4879c365.png)
예약 후 - 마일리지 상태
![image](https://user-images.githubusercontent.com/15603058/121325713-cd0fe300-c94c-11eb-916c-82e06059af70.png)
예약 취소
![image](https://user-images.githubusercontent.com/15603058/121326269-5b846480-c94d-11eb-9f83-9de0b5bf2550.png)
예약 후 - 결제 상태
![image](https://user-images.githubusercontent.com/15603058/121326400-7c4cba00-c94d-11eb-817b-ea5dcc7e433e.png)
예약 후 - 마일리지 상태
![image](https://user-images.githubusercontent.com/15603058/121326446-8a9ad600-c94d-11eb-88f6-1cb7e4b6f63b.png)


## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다. (예시는 mileage 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하려고 노력했다. 현실에서 발생가는한 이벤트에 의하여 마이크로 서비스들이 상호 작용하기 좋은 모델링으로 구현을 하였다.

```
package airbnb;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Mileage_table")
public class Mileage {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long mileageId;
    private Long roomId;
    private Long payId;
    private Integer mileagePoint;
    private String status;

    @PostPersist
    public void onPostPersist(){
        ////////////////////////////
        // 결제 승인 된 경우 - 마일리지 증가
        ////////////////////////////

        // 이벤트 발행 -> MileageIncreased        
        MileageIncreased mileageIncreased = new MileageIncreased();
        
        BeanUtils.copyProperties(this, mileageIncreased);
        mileageIncreased.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){

        //////////////////////
        // 결제 취소 된 경우 - 마일리지 감소
        //////////////////////

        // 이벤트 발행 -> MileageDecreased        
        MileageDecreased mileageDecreased = new MileageDecreased();
        BeanUtils.copyProperties(this, mileageDecreased);
        mileageDecreased.publishAfterCommit();
    }

    public Long getMileageId() {
        return mileageId;
    }

    public void setMileageId(Long mileageId) {
        this.mileageId = mileageId;
    }
    
    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
    
    public Integer getMileagePoint() {
        return mileagePoint;
    }

    public void setMileagePoint(Integer mileagePoint) {
        this.mileagePoint = mileagePoint;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package airbnb;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="mileages", path="mileages")
public interface MileageRepository extends PagingAndSortingRepository<Mileage, Long>{


}
```
- 적용 후 REST API 의 테스트
```
# room 서비스의 room 등록
http POST http://localhost:8088/rooms desc="Beautiful House"
http POST http://a84ad56482306476ca2c15d1d2a7682e-1946155919.ap-northeast-2.elb.amazonaws.com:8080/rooms desc="Personal Final House"

# reservation 서비스의 예약 요청
http POST http://localhost:8088/reservations roomId=1 status=reqReserve
http POST http://a84ad56482306476ca2c15d1d2a7682e-1946155919.ap-northeast-2.elb.amazonaws.com:8080/reservations roomId=1 status=reqReserve

# reservation 서비스의 예약 상태 확인
http GET http://localhost:8088/reservations
http GET http://a84ad56482306476ca2c15d1d2a7682e-1946155919.ap-northeast-2.elb.amazonaws.com:8080/payments/1

# reservation 서비스의 예약 취소 요청
http PATCH http://localhost:8088/reservations/1 status=reqCancel
http PATCH http://a84ad56482306476ca2c15d1d2a7682e-1946155919.ap-northeast-2.elb.amazonaws.com:8080/reservations/1 status=reqCancel

```

## 동기식 호출(Sync) 과 Fallback 처리

분석 단계에서의 조건 중 하나로 결제 후 마일리지 적립은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

- 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
# MileageService.java

package airbnb.external;

<import문 생략>

@FeignClient(name="Mileage", url="${prop.gateway.url}")
public interface MileageService {

    @RequestMapping(method= RequestMethod.POST, path="/mileages")
    public void increaseMileage(@RequestBody Mileage mileage);

}


```

- 예약 요청을 받은 직후(@PostPersist) 가능상태 확인 및 결제를 동기(Sync)로 요청하도록 처리
```
# Payment.java (Entity)

    @PostPersist
    public void onPostPersist(){
        ////////////////////////////
        // 결제 승인 된 경우
        ////////////////////////////
                
        // 이벤트 발행 -> PaymentApproved
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();

        //////////////////////////////
        // Mileage 적립 진행 (POST방식)
        //////////////////////////////
        airbnb.external.Mileage mileage = new airbnb.external.Mileage();
        mileage.setRoomId(this.getRoomId());
        mileage.setPayId(this.getPayId());
        mileage.setMileagePoint(1);
        mileage.setStatus("Mileage Incresed");
        PaymentApplication.applicationContext.getBean(airbnb.external.MileageService.class)
            .increaseMileage(mileage);
        
        System.out.println("== Added mileageId : " + mileage.getMileageId());
        System.out.println("== Added payId     : " + mileage.getPayId());

    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


```
# 확인(마일리지 서비스 중지 후 예약 요청, 마일리지 서비스 실행 후 예약 요청->결제->마일리지적립)

D:\LV2\Source\airbnb_Final\mileage>http POST http://localhost:8088/reservations roomId=1 status=reqReserve
HTTP/1.1 500 Internal Server Error
Content-Type: application/json;charset=UTF-8
Date: Wed, 09 Jun 2021 09:52:54 GMT
transfer-encoding: chunked

{
    "error": "Internal Server Error",
    "message": "Could not commit JPA transaction; nested exception is javax.persistence.RollbackException: Error while committing the transaction",
    "path": "/reservations",
    "status": 500,
    "timestamp": "2021-06-09T09:52:54.359+0000"
}



D:\LV2\Source\airbnb_Final\mileage>http POST http://localhost:8088/reservations roomId=1 status=reqReserve
HTTP/1.1 201 Created
Content-Type: application/json;charset=UTF-8
Date: Wed, 09 Jun 2021 09:54:14 GMT
Location: http://localhost:8083/reservations/3
transfer-encoding: chunked

{
    "_links": {
        "reservation": {
            "href": "http://localhost:8083/reservations/3"
        },
        "self": {
            "href": "http://localhost:8083/reservations/3"
        }
    },
    "payId": null,
    "roomId": 1,
    "status": "reqReserve"
}
```

- 또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)





## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트


동기식 호출을 제외한 나머지는 비동기로 호출한다.
예) 1. 결제 취소 시 결제 취소 이벤트 Publish
    2. Subscribe 후 마일리지 적립 취소 후 취소 이벤트를 카프카로 Publish
    3. Subscribe 후 message 서비스에서 처리
  
```
# Payment.java

package airbnb;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name="Payment_table")
public class Payment {

    ....

    @PostUpdate
    public void onPostUpdate(){

        //////////////////////
        // 결제 취소 된 경우
        //////////////////////
        System.out.println("================= this.getStatus()     : " + this.getStatus());
        
        if(this.getStatus().equals("cancelled")) {
            // 이벤트 발행 -> PaymentCancelled
            PaymentCancelled paymentCancelled = new PaymentCancelled();
            BeanUtils.copyProperties(this, paymentCancelled);
            paymentCancelled.publishAfterCommit();
        }
    }
    
    ....
}
```

- 마일리지 시스템에서는 결제 취소 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
# 

package airbnb;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_DecreaseMileage(@Payload PaymentCancelled paymentCancelled){

        if(paymentCancelled.isMe()){

            /////////////////////////////////////////////
            // 결제 취소 요청이 왔을 때 -> status -> cancelled 
            /////////////////////////////////////////////
            System.out.println("\n\n##### listener DecreaseMileage : " + paymentCancelled.toJson() + "\n\n");
            
            // 취소시킬 mileage 추출
            long mileageId = paymentCancelled.getMileageId(); // 취소시킬 mileage ID

            Optional<Mileage> res = mileageRepository.findById(mileageId);
            Mileage mileage = res.get();

            mileage.setMileagePoint(mileage.getMileagePoint()); // 마일리지 감소양
            mileage.setStatus("Mileage Canceled"); // 마일리지 감소
            mileage.setMileagePoint(0);

            System.out.println("Edited mileageID    : " + mileage.getMileageId());
            System.out.println("Edited roomID       : " + mileage.getRoomId());
            System.out.println("Edited payID        : " + mileage.getPayId());
            System.out.println("Edited mileagePoint : " + mileage.getMileagePoint());
            System.out.println("Edited status       : " + mileage.getStatus());

            // DB Update
            mileageRepository.save(mileage);

        }
    }

```

그 외 메시지 서비스는 예약/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 메시지 서비스가 유지보수로 인해 잠시 내려간 상태 라도 예약을 받는데 문제가 없다.

```
# 메시지 서비스 (message) 를 잠시 내려놓음 (ctrl+c)

# 예약 요청
http POST http://localhost:8088/reservations roomId=1 status=reqReserve   #Success

# 예약 상태 확인
http GET localhost:8088/reservations    #메시지 서비스와 상관없이 예약 상태는 정상 확인

```

# 운영


## CI/CD 설정

각 구현체들은 각자의 source repository 에 구성되었고, 사용한 CI/CD는 buildspec.yml을 이용한 AWS codebuild를 사용하였습니다.

- CodeBuild 프로젝트를 생성하고 AWS_ACCOUNT_ID, KUBE_URL, KUBE_TOKEN 환경 변수 세팅을 한다
```
SA 생성
kubectl apply -f eks-admin-service-account.yml
```
![codebuild(sa)](https://user-images.githubusercontent.com/38099203/119293259-ff52ec80-bc8c-11eb-8671-b9a226811762.PNG)
```
Role 생성
kubectl apply -f eks-admin-cluster-role-binding.yml
```
![codebuild(role)](https://user-images.githubusercontent.com/38099203/119293300-1abdf780-bc8d-11eb-9b07-ad173237efb1.PNG)
```
Token 확인
kubectl -n kube-system get secret
kubectl -n kube-system describe secret eks-admin-token-q82sg
```
![image](https://user-images.githubusercontent.com/15603058/121338667-ac4d8a80-c958-11eb-8f8d-915f72774351.png)
```
buildspec.yml 파일 
마이크로 서비스 room의 yml 파일 이용하도록 세팅
```
![codebuild(buildspec)](https://user-images.githubusercontent.com/38099203/119283849-30292680-bc79-11eb-9f86-cbb715e74846.PNG)

- codebuild 실행
```
codebuild 프로젝트 및 빌드 이력
```
![image](https://user-images.githubusercontent.com/15603058/121349903-005e6c00-c965-11eb-9679-cf47c4b3a80c.png)

- codebuild 빌드 내역 (Message 서비스 세부)

![image](https://user-images.githubusercontent.com/15603058/121350099-34d22800-c965-11eb-92cd-79da6a693aa6.png)


## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: istio 사용하여 구현함

시나리오는 예약(reservation)--> 룸(room) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리.

- DestinationRule 를 생성하여 circuit break 가 발생할 수 있도록 설정
최소 connection pool 설정
```
# destination-rule.yml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: dr-mileage
  namespace: airbnb
spec:
  host: mileage
  trafficPolicy:
    connectionPool:
      http:
        http1MaxPendingRequests: 1
        maxRequestsPerConnection: 1
#    outlierDetection:
#      interval: 1s
#      consecutiveErrors: 1
#      baseEjectionTime: 10s
#      maxEjectionPercent: 100
```

* istio-injection 활성화 및 room pod container 확인

```
kubectl get ns -L istio-injection
kubectl label namespace airbnb istio-injection=enabled 
```

![image](https://user-images.githubusercontent.com/15603058/121355199-98ab1f80-c96a-11eb-8a67-2eabf54aade9.png)

![image](https://user-images.githubusercontent.com/15603058/121355032-70bbbc00-c96a-11eb-8f03-3f18f1078391.png)


* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:

siege 실행

```
kubectl run siege --image=apexacme/siege-nginx -n airbnb
kubectl exec -it siege-5c7c46b788-svq6c -c siege -n airbnb -- /bin/bash
```


- 동시사용자 1로 부하 생성 시 모두 정상
```
siege -c1 -t10S -v --content-type "application/json" 'http://mileage:8080/mileages POST {"roomId": "1", "payId": "1", "mileagePoint": "1", "status" : "Mileage Increased"}'

** SIEGE 4.0.4
** Preparing 1 concurrent users for battle.
The server is now under siege...
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
```

- 동시사용자 2로 부하 생성 시 503 에러 168개 발생
```
siege -c2 -t10S -v --content-type "application/json" 'http://mileage:8080/mileages POST {"roomId": "1", "payId": "1", "mileagePoint": "1", "status" : "Mileage Increased"}'

** SIEGE 4.0.4
** Preparing 2 concurrent users for battle.
The server is now under siege...
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 503     0.00 secs:      81 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.03 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.02 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 503     0.01 secs:      81 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 503     0.00 secs:      81 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 503     0.00 secs:      81 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 503     0.00 secs:      81 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 503     0.01 secs:      81 bytes ==> POST http://mileage:8080/mileages

Lifting the server siege...
Transactions:                   1344 hits
Availability:                  80.67 %
Elapsed time:                   9.75 secs
Data transferred:               0.35 MB
Response time:                  0.01 secs
Transaction rate:             137.85 trans/sec
Throughput:                     0.04 MB/sec
Concurrency:                    1.96
Successful transactions:        1344
Failed transactions:             322
Longest transaction:            0.07
Shortest transaction:           0.00
```

- kiali 화면에 서킷 브레이크 확인

![image](https://user-images.githubusercontent.com/15603058/121354799-33efc500-c96a-11eb-895b-641b3c9b013b.png)


- 다시 최소 Connection pool로 부하 다시 정상 확인

```
** SIEGE 4.0.4
** Preparing 1 concurrent users for battle.
The server is now under siege...
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.02 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.00 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.02 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.02 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.02 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.00 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.02 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.00 secs:     257 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.01 secs:     257 bytes ==> POST http://mileage:8080/mileages

Lifting the server siege...
Transactions:                    828 hits
Availability:                 100.00 %
Elapsed time:                   9.95 secs
Data transferred:               0.20 MB
Response time:                  0.01 secs
Transaction rate:              83.22 trans/sec
Throughput:                     0.02 MB/sec
Concurrency:                    0.98
Successful transactions:         828
Failed transactions:               0
Longest transaction:            0.06
Shortest transaction:           0.00

```

- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌.
  virtualhost 설정과 동적 Scale out (replica의 자동적 추가,HPA) 을 통하여 시스템을 확장 해주는 후속처리가 필요.


### 오토스케일 아웃
앞서 CB 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다. 

- mileage deployment.yml 파일에 resources 설정을 추가한다
![image](https://user-images.githubusercontent.com/15603058/121358945-f856fa00-c96d-11eb-9816-3b6878075523.png)

- room 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 50프로를 넘어서면 replica 를 10개까지 늘려준다:
```
kubectl autoscale deployment room -n airbnb --cpu-percent=50 --min=1 --max=10
```
![Autoscale (HPA)(kubectl autoscale 명령어)](https://user-images.githubusercontent.com/38099203/119299474-ec92e480-bc99-11eb-9bc3-8c5246b02783.PNG)

- 부하를 동시사용자 200명, 1분 동안 걸어준다.
```
siege -c200 -t60S -v --content-type "application/json" 'http://room:8080/rooms POST {"desc": "Beautiful House3"}'
```
- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```
kubectl get deploy room -w -n airbnb 
```
- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:
![image](https://user-images.githubusercontent.com/15603058/121358813-da899500-c96d-11eb-930a-b02bc0f9fd0f.png)


## 무정지 재배포

* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함

```
kubectl delete destinationrules dr-room -n airbnb
kubectl label namespace airbnb istio-injection-
kubectl delete hpa room -n airbnb
```

- seige 로 배포작업 직전에 워크로드를 모니터링 함.
```
root@siege-5c7c46b788-svq6c:/# siege -c100 -t60S -v --content-type "application/json" 'http://mileage:8080/mileages POST {"roomId": "1", "payId": "1", "mileagePoint": "1", "status" : "Mileage Increased"}'
** SIEGE 4.0.4
** Preparing 100 concurrent users for battle.
The server is now under siege...
HTTP/1.1 201     0.04 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.05 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.06 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.07 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.08 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.09 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.09 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.08 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.08 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.09 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.09 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.11 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.09 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.10 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.09 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.10 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.11 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.10 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.11 secs:     259 bytes ==> POST http://mileage:8080/mileages
HTTP/1.1 201     0.10 secs:     259 bytes ==> POST http://mileage:8080/mileages

```

- 새버전으로의 배포 시작
```
kubectl set image ...
```

- seige 의 화면으로 넘어가서 Availability 가 100% 미만으로 떨어졌는지 확인

```
root@siege-5c7c46b788-svq6c:/# siege -c100 -t60S -v --content-type "application/json" 'http://mileage:8080/mileages POST {"roomId": "1", "payId": "1", "mileagePoint": "1", "status" : "Mileage Increased"}'


Transactions:                   8250 hits
Availability:                  86.32 %
Elapsed time:                  21.86 secs
Data transferred:               2.04 MB
Response time:                  0.52 secs
Transaction rate:             377.44 trans/sec
Throughput:                     0.09 MB/sec
Concurrency:                   81.21
Successful transactions:        7732
Failed transactions:            1123
Longest transaction:            0.94
Shortest transaction:           0.00

```
- 배포기간중 Availability 가 평소 100%에서 87% 대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY 상태로 인식하여 서비스 유입을 진행한 것이기 때문. 이를 막기위해 Readiness Probe 를 설정함

```
# deployment.yaml 의 readiness probe 의 설정:
```

![image](https://user-images.githubusercontent.com/15603058/121364529-d318ba80-c972-11eb-9120-01e3935b5202.png)

```
kubectl apply -f kubernetes/deployment.yml
```

- 동일한 시나리오로 재배포 한 후 Availability 확인:
```
Lifting the server siege...
Transactions:                  14586 hits
Availability:                 100.00 %
Elapsed time:                  59.73 secs
Data transferred:               3.58 MB
Response time:                  0.41 secs
Transaction rate:             244.20 trans/sec
Throughput:                     0.06 MB/sec
Concurrency:                   99.37
Successful transactions:       14586
Failed transactions:               0
Longest transaction:            2.41
Shortest transaction:           0.00

```

배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.


# Self-healing (Liveness Probe)
- mileage deployment.yml 파일 수정 
```
콘테이너 실행 후 /tmp/healthy 파일을 만들고 
90초 후 삭제
livenessProbe에 'cat /tmp/healthy'으로 검증하도록 함
```
![image](https://user-images.githubusercontent.com/15603058/121365690-d496b280-c973-11eb-950b-2ac55e23c11b.png)

- kubectl describe pod room -n airbnb 실행으로 확인
```
컨테이너 실행 후 90초 동인은 정상이나 이후 /tmp/healthy 파일이 삭제되어 livenessProbe에서 실패를 리턴하게 됨
pod 정상 상태 일때 pod 진입하여 /tmp/healthy 파일 생성해주면 정상 상태 유지됨
```

![image](https://user-images.githubusercontent.com/15603058/121366736-abc2ed00-c974-11eb-9a2c-49881300b9d8.png)
![touch tmp healthy](https://user-images.githubusercontent.com/38099203/119319050-f118c680-bcb4-11eb-8bca-aa135c1e067e.PNG)

