package com.formacoach.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun PageHeading(title:Int,subtitle:Int?=null) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title),style=MaterialTheme.typography.headlineMedium)
        subtitle?.let {Text(stringResource(it),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable
internal fun EmptyState(title:Int,body:Int) {
    Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Insights,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
            Text(stringResource(title),style=MaterialTheme.typography.titleMedium)
            Text(stringResource(body),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MetricTile(value:String,label:String,modifier:Modifier=Modifier) {
    Surface(modifier=modifier,color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.45f),shape=RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Text(value,style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.onSurface)
            Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun FormFieldPair(first:@Composable ()->Unit,second:@Composable ()->Unit) {
    val largeText=LocalDensity.current.fontScale>1.25f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if(largeText||maxWidth<290.dp) Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {first();second()}
        else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {first()}
            Column(Modifier.weight(1f)) {second()}
        }
    }
}
