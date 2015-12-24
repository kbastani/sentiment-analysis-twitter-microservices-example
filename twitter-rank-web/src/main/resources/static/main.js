$(function () {
// ignore this first line (its fidle mock) and it will return what ever you pass as json:... parameter... consider to change it to your ajax call
    $.ajax({
        url: '/twitter-rank/users/search/findRankedUsers?skip=0&limit=100',
        type: "get",
        dataType: "json",
        success: function (data, textStatus, jqXHR) {
            // since we are using jQuery, you don't need to parse response
            drawTable(data._embedded.users);
        }
    });

    function drawTable(data) {
        for (var i = 0; i < data.length; i++) {
            drawRow(data[i]);
        }
    }

    function drawRow(rowData) {
        var row = $("<tr />")
        $("#personDataTable").append(row); //this will append tr element to table... keep its reference for a while since we will add cels into it

        row.append($("<td><img src='" + rowData.profileImageUrl + "'/></td>"));
        row.append($("<td>" + rowData.name + "</td>"));
        row.append($("<td><a href='http://www.twitter.com/" + rowData.screenName + "' target='_blank'>@" + rowData.screenName + "</a></td>"));
        row.append($("<td>" + rowData.followsCount + "</td>"));
        row.append($("<td>" + rowData.followerCount + "</td>"));
        row.append($("<td>" + rowData.pagerank + "</td>"));

        var change = (((rowData.previousRank == 0) ? rowData.currentRank : rowData.previousRank) - rowData.currentRank);
        var arrowChange = change > 0 ? "fa fa-caret-up" : change < 0 ? "fa fa-caret-down" : (rowData.previousRank == 0) ? "fa fa-caret-up" : "fa fa-minus";

        row.append($("<td><i class='" + arrowChange + "' aria-hidden='true'></i></td>"));
    }
});