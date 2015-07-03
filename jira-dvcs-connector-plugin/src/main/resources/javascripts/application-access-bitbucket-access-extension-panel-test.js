AJS.test.require("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:application-access-bitbucket-access-component", function () {
    "use strict";

    require([
        'jquery',
        'application-access-bitbucket-access-extension-panel'
    ], function(
            $,
            Panel
    ) {
        module('Application Role Defaults BB Panel', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                this.mockDivId = "mock_div_id"
                this.selector = "div#" + this.mockDivId;

                this.model = {
                    get: this.sandbox.stub(),
                    on: this.sandbox.stub()
                };

                $('body').append($("<div id='" + this.mockDivId + "' />"));
            },


            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Panel should be visible when checked is true", function() {
            this.model.get.withArgs("checked").returns(true);
            this.model.on.callsArg(1);
            this.panel = new Panel({model: this.model, el: this.selector});

            equal($(this.selector).is(":visible"), true);
        });

        test("Panel should be hidden when checked is false", function() {
            this.model.get.withArgs("checked").returns(false);
            this.model.on.callsArg(1);
            this.panel = new Panel({model: this.model, el: this.selector});

            equal($(this.selector).is(":visible"), false);
        });

    })
});