package org.vlog.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.vlog.app.R
import org.vlog.app.data.model.FolderItem


@Composable
fun FolderItemGridLayout(
	foldersList: List<FolderItem>,
	onFolderItemClick: (FolderItem) -> Unit,
	contentPadding: PaddingValues,
	modifier: Modifier = Modifier
){
	LazyVerticalGrid(
		modifier = modifier,
		contentPadding = contentPadding,
		horizontalArrangement = Arrangement.Center,
		verticalArrangement = Arrangement.Top,
		columns = GridCells.Fixed(2)
	){

		items(foldersList, key = {it.name}){folderItem ->
			FolderGridItem(folderItem = folderItem, onItemClick = onFolderItemClick)
		}

	}
}

@Composable
private fun FolderGridItem(
	folderItem: FolderItem,
	onItemClick: (FolderItem) -> Unit,
	modifier: Modifier = Modifier
){
	Column(
		modifier = modifier
			.clickable(
				onClick = {
					onItemClick(folderItem)
				}
			),
		verticalArrangement = Arrangement.Center,
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Icon(
			imageVector = Icons.Default.Lock,
			contentDescription = "folder",
			tint = MaterialTheme.colorScheme.primary,
			modifier = Modifier.size(200.dp)
		)
		Text(
			text = folderItem.name,
			color = MaterialTheme.colorScheme.primary,
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Start,
			maxLines = 3,
			overflow = TextOverflow.Ellipsis,
		)
	}
}