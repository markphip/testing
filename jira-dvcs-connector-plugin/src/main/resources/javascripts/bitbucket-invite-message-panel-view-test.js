AJS.test.require("com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:application-access-bitbucket-access-component", function () {
    "use strict";

    require([
        'jquery',
        'bitbucket-invite-message-panel'
    ], function(
            $,
            Panel
    ) {
        module('Application Role Defaults BB Panel', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                this.panelId = "mock_div_id"
                this.selector = "div#" + this.panelId;

                this.model = {
                    get: this.sandbox.stub(),
                    on: this.sandbox.stub()
                };

                $('#qunit-fixture').append($("<div id='" + this.panelId + "' />"));
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