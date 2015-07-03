AJS.test.require(["com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:application-access-bitbucket-access-component",
   "jira.webresources:application-roles"], function () {
    "use strict";

    require([
        'jquery',
        'application-access-bitbucket-access-extension-panel-model',
        'jira/admin/application/defaults/api'
    ], function(
            $,
            PanelModel,
            defaultsAPI
    ) {
        module('Application Role Defaults BB Panel Model', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                // mock the dialogView
                this.dialog = {};
                this.dialog.$el = {};
                this.dialog.$el = {
                    on: this.sandbox.stub(),
                    attr: this.sandbox.stub()
                };
                this.dialog.$el.attr.withArgs('id').returns('app-role-defaults-dialog');

                this.dialog.collection = {}
                this.dialog.collection.models = [];
                this.dialog.collection.models[0] = {
                    toJSON: this.sandbox.stub()
                };

                this.jiraSoftwareCheckboxSelector = "#checkbox";

                this.mockCheckboxModel = function(checked){
                    this.dialog.collection.models[0].toJSON.returns({
                        key: 'jira-software',
                        selectedByDefault: checked
                    });
                }

            },

            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Model should set checked to true when checkbox is checked", function() {
            this.mockCheckboxModel(true);
            var model = new PanelModel({jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector});
            defaultsAPI.trigger(defaultsAPI.EVENT_ON_SHOW, this.dialog);

            equal(model.get("checked"), true);

        });

        test("Model should set checked to false when checkbox is not checked", function() {
            this.mockCheckboxModel(false);
            var model = new PanelModel({jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector});
            defaultsAPI.trigger(defaultsAPI.EVENT_ON_SHOW, this.dialog);

            equal(model.get("checked"), false);

        });

    })
});