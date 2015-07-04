AJS.test.require(["com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:bitbucket-invite-message-panel-component",
   "jira.webresources:application-roles"], function () {
    "use strict";

    require([
        'jquery',
        'bitbucket-invite-message-panel-model',
        'jira/admin/application/defaults/api'
    ], function(
            $,
            PanelModel,
            defaultsAPI
    ) {
        module('Application Role Defaults BB Panel Model', {
            setup: function() {
                this.sandbox = sinon.sandbox.create();

                this.jiraSoftwareCheckboxSelector = "#checkbox";
                // mock the dialogView
                this.dialog = {};
                this.dialog.$el = {};
                this.dialog.$el = {
                    attr: this.sandbox.stub(),
                    find: this.sandbox.stub()
                };

                this.checkbox = {
                    on: this.sandbox.stub(),
                    attr: this.sandbox.stub()
                };
                this.checkbox.on.callsArg(1);

                this.dialog.$el.attr.withArgs('id').returns('id');
                this.dialog.$el.find.withArgs(this.jiraSoftwareCheckboxSelector).returns(this.checkbox);

            },

            teardown: function() {
                this.sandbox.restore();
            }
        });

        test("Model should set checked to true when checkbox is checked", function() {
            this.checkbox.attr.withArgs("checked").returns(true);
            var model = new PanelModel({
                jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector,
                jiraSoftwareCheckboxContainerId: 'id'});

            defaultsAPI.trigger(defaultsAPI.EVENT_ON_SHOW, this.dialog);

            equal(model.get("checked"), true);

        });

        test("Model should set checked to false when checkbox is not checked", function() {
            this.checkbox.attr.withArgs("checked").returns(false);
            var model = new PanelModel({
                jiraSoftwareCheckboxSelector: this.jiraSoftwareCheckboxSelector,
                jiraSoftwareCheckboxContainerId: 'id'});

            defaultsAPI.trigger(defaultsAPI.EVENT_ON_SHOW, this.dialog);

            equal(model.get("checked"), false);
        });

    })
});