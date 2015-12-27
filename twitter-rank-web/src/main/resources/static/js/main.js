$(function () {

    $('#myModal').on('shown.bs.modal', function () {
        $('#myInput').focus()
        $('#add-profile-form').bootstrapValidator('resetForm', true);
    })

    $('#add-profile-form').bootstrapValidator({
        feedbackIcons: {
            valid: 'glyphicon glyphicon-ok',
            invalid: 'glyphicon glyphicon-remove',
            validating: 'glyphicon glyphicon-refresh'
        },
        submitHandler: function (validator, form, submitButton) {
            var screenName = $("#twitterHandle").val();
            $(".loader").show();

            $.ajax({
                url: '/twitter-rank/v1/user/' + screenName,
                type: "get",
                dataType: "json",
                success: function (data) {
                    $(".loader").hide();
                    $('#myModal').modal("hide");
                    $("#dashboard-header").html("");
                    location.reload();
                },
                error: function (xhr, status) {
                    $(".loader").hide();
                    $("#add-profile-form").data('bootstrapValidator').updateStatus("twitterHandle", "INVALID", "callback")
                }
            });
        },
        fields: {
            twitterHandle: {
                validators: {
                    notEmpty: {
                        message: 'A valid Twitter profile handle is required'
                    },
                    callback: {
                        message: 'An error occurred',
                        callback: function (value, validator) {
                            return true;
                        }
                    }
                }
            }
        }
    });

    $.ajax({
        url: '/twitter-rank/users/search/findRankedUsers?skip=0&limit=100',
        type: "get",
        dataType: "json",
        success: function (data) {
            $(".loader").hide();
            drawTable(data._embedded.users);
            drawDashboardHeader(data._embedded.users)
        },
        error: function (xhr, status) {
            $(".glyphicon-refresh")
                .removeClass("glyphicon-refresh")
                .removeClass("glyphicon-refresh-animate")
                .addClass("glyphicon-exclamation-sign")
        }
    });

    function drawTable(data) {
        for (var i = 0; i < data.length; i++) {
            drawRow(data[i]);
        }
    }

    function drawDashboardHeader(data) {

        data.sort(function (a, b) {
            return a.discoveredRank - b.discoveredRank;
        });

        for (var i = 0; i < Math.min(data.length, 3); i++) {
            drawSpotlight(data[i]);
        }
    }

    function drawSpotlight(rowData) {
        var row = $('<div class="col-sm-4 placeholder"><img src="' + rowData.profileImageUrl.replace("_normal", "") + '" class="img-responsive img-profile-dashboard" alt="Generic placeholder thumbnail"><h4 class="screen-name">@' + rowData.screenName + '</h4> <span class="text-muted full-name">' + rowData.name + '</span></div>')
        $("#dashboard-header").append(row);
    }

    function drawRow(rowData) {
        var row = $("<tr />")
        $("#personDataTable").append(row);

        var currentRank = rowData.currentRank == null ? "<i class='fa fa-plus'></i>" : rowData.currentRank + ".";

        row.append($("<td class='rank-col'>" + currentRank + "</td>"));
        row.append($("<td><img src='" + rowData.profileImageUrl + "'/></td>"));
        row.append($("<td class='hidden-xs'>" + rowData.name + "</td>"));
        row.append($("<td><a href='http://www.twitter.com/" + rowData.screenName + "' target='_blank'>@" + rowData.screenName + "</a></td>"));
        row.append($("<td class='hidden-xs'>" + rowData.followsCount + "</td>"));
        row.append($("<td class='hidden-xs'>" + rowData.followerCount + "</td>"));
        row.append($("<td class='hidden-xs'>" + rowData.pagerank + "</td>"));

        var change = (((rowData.previousRank == 0) ? rowData.currentRank : rowData.previousRank) - rowData.currentRank);
        var arrowChange = change > 0 ? "fa fa-caret-up" : change < 0 ? "fa fa-caret-down" : (rowData.previousRank == 0) ? "fa fa-caret-up" : "fa fa-minus";

        row.append($("<td class='hidden-xs'><i class='" + arrowChange + "' aria-hidden='true'></i></td>"));
    }
});