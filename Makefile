PROJECT = DND-App

SRC = $(shell find src -type f -iname '*.kt')
BUILD_DIR = output

KOTLINC = kotlinc
KOTLINC_ARGS = -include-runtime
KOTLINC_ARGS += -d $(BUILD_DIR)/$(PROJECT).jar

run: $(BUILD_DIR)/$(PROJECT).jar
	cd $(BUILD_DIR) && java -jar $(PROJECT).jar

build: $(BUILD_DIR)/$(PROJECT).jar

$(BUILD_DIR)/$(PROJECT).jar: $(BUILD_DIR) $(SRC)
	$(KOTLINC) $(SRC) $(KOTLINC_ARGS)

clean:
	@rm -vrf $(BUILD_DIR)

$(BUILD_DIR):
	@mkdir -vp $(BUILD_DIR)
