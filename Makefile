all: rtl test

rtl:
	sbt "runMain STA_Gen.SystolicTensorArrayRtlMain"

test:
	sbt "test"

clean:
	rm *.json *.v